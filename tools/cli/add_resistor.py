#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any
from urllib.parse import urlencode

import requests

SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

import add_item


@dataclass
class ResistorMatch:
    part_number: str
    resistance: str
    resistance_key: int
    package_name: str | None
    tolerance: str | None
    stock: int
    price: float | None
    fetched: add_item.FetchedComponent


def main() -> int:
    args = parse_args()
    colors = add_item.Colors(enabled=not args.no_color and sys.stdout.isatty())
    workbook_path = Path(args.file).expanduser()
    if not workbook_path.exists():
        raise add_item.WorkbookError(f"file not found: {workbook_path}")

    package_name = normalize_package(args.package)
    tolerance = normalize_tolerance(args.tolerance)
    resistance_values = [normalize_resistance(value) for value in args.values]
    now_ms = int(time.time() * 1000)

    matches = []
    if args.no_fetch:
        raise add_item.WorkbookError("add_resistor.py requires LCSC search; remove --no-fetch")
    for resistance in resistance_values:
        match = find_resistor_match(
            resistance=resistance,
            package_name=package_name,
            tolerance=tolerance,
            timeout=args.timeout,
        )
        if match is None:
            raise add_item.WorkbookError(
                f"no resistor match found for {resistance.display} {package_name} {tolerance.display}"
            )
        matches.append(match)
        print(
            f"match: {colors.cyan(resistance.display)} -> {colors.green(match.part_number)}; "
            f"mpn={match.fetched.name or '-'}, package={match.package_name or '-'}, "
            f"tolerance={match.tolerance or '-'}, stock={match.stock}"
        )

    workbook = add_item.load_workbook(workbook_path)
    try:
        sheets = add_item.load_inventory_workbook(workbook)
        location_profiles = add_item.build_location_category_lookup(sheets)
        add_item.print_location_category_profiles(sheets, location_profiles, colors)

        for match in matches:
            request = add_item.InboundRequest(
                part_number=match.part_number,
                count=args.count,
                location=args.location.strip().upper() if args.location else None,
            )
            result = process_resistor_request(
                request=request,
                fetched=match.fetched,
                sheets=sheets,
                location_profiles=location_profiles,
                fallback_location=args.fallback_location.upper(),
                now_ms=now_ms,
            )
            if not args.dry_run:
                result.image_status = add_item.insert_component_preview_image(
                    sheet=sheets.components_sheet,
                    headers=sheets.components_headers,
                    row=result.component_row,
                    image_url=result.fetched.image_url if result.fetched else None,
                    timeout=args.timeout,
                )
            add_item.print_inbound_result(
                result=result,
                dry_run=args.dry_run,
                colors=colors,
                show_fetch_note=False,
            )
            location_profiles = add_item.build_location_category_lookup(sheets)

        output_path = None
        if not args.dry_run:
            output_path = add_item.build_output_path(
                source_path=workbook_path,
                label=build_output_label(package_name, tolerance, resistance_values),
                output_dir=Path(args.output_dir).expanduser(),
                now_ms=now_ms,
            )
            output_path.parent.mkdir(parents=True, exist_ok=True)
            workbook.save(output_path)

        if output_path is not None:
            print(f"saved: {colors.green(output_path)}")
        return 0
    finally:
        workbook.close()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Find common resistor LCSC numbers by package/tolerance/value and add them to a backup workbook."
    )
    parser.add_argument("--file", required=True, help="Path to lcsc_inventory_backup_*.xlsx")
    parser.add_argument("-p", "--package", required=True, help="Resistor package, for example 0402 or 0603")
    parser.add_argument("-t", "--tolerance", required=True, help="Tolerance, for example 1%%, 0.1%%, ±1%%")
    parser.add_argument("-c", "--count", type=int, default=1, help="Quantity to add for each value. Default: 1")
    parser.add_argument("-l", "--location", help="Override target location for every value")
    parser.add_argument("--fallback-location", default="A1", help="Fallback location. Default: A1")
    parser.add_argument("--timeout", type=float, default=10.0, help="LCSC request timeout in seconds")
    parser.add_argument("--dry-run", action="store_true", help="Print the result without saving the workbook")
    parser.add_argument("--no-color", action="store_true", help="Disable ANSI color output")
    parser.add_argument("--no-fetch", action="store_true", help="Reserved; resistor lookup requires network search")
    parser.add_argument(
        "--output-dir",
        default=str(add_item.DEFAULT_OUTPUT_DIR),
        help="Directory for the generated workbook. Default: tools/cli/output",
    )
    parser.add_argument("values", nargs="+", help="Resistance values, for example 1.5K 2.2M 2.2r 100k")
    args = parser.parse_args()
    if args.count <= 0:
        raise add_item.WorkbookError("--count must be greater than 0")
    return args


@dataclass(frozen=True)
class NormalizedResistance:
    display: str
    ohms_milli: int


@dataclass(frozen=True)
class NormalizedTolerance:
    display: str
    basis_points: int


def process_resistor_request(
    request: add_item.InboundRequest,
    fetched: add_item.FetchedComponent,
    sheets: add_item.WorkbookSheets,
    location_profiles: dict[int, tuple[str | None, str | None]],
    fallback_location: str,
    now_ms: int,
) -> add_item.InboundResult:
    component = sheets.components_by_part_number.get(request.part_number)
    if component is None:
        component, component_row = add_item.append_component(
            sheets=sheets,
            part_number=request.part_number,
            fetched=fetched,
            now_ms=now_ms,
        )
        component_status = "created"
    else:
        component, component_row = add_item.enrich_existing_component(
            sheets=sheets,
            component=component,
            fetched=fetched,
            now_ms=now_ms,
        )
        component_status = "updated"

    location_code = request.location or add_item.suggest_inbound_location_code(
        component=component,
        sheets=sheets,
        location_profiles=location_profiles,
        fallback_code=fallback_location,
    )
    location = add_item.find_or_create_location(
        sheets=sheets,
        code=location_code,
        now_ms=now_ms,
    )
    inventory_status, quantity_after = add_item.add_inventory_quantity(
        sheets=sheets,
        component_id=component.id,
        location_id=location.id,
        quantity_delta=request.count,
        now_ms=now_ms,
    )
    return add_item.InboundResult(
        part_number=request.part_number,
        count=request.count,
        location_code=location.code,
        component_status=component_status,
        inventory_status=inventory_status,
        quantity_after=quantity_after,
        fetched=fetched,
        component_row=component_row,
    )


def find_resistor_match(
    resistance: NormalizedResistance,
    package_name: str,
    tolerance: NormalizedTolerance,
    timeout: float,
) -> ResistorMatch | None:
    records = search_lcsc_products(
        keyword=f"{resistance.display} {tolerance.display} {package_name} 电阻",
        timeout=timeout,
    )
    candidates = []
    for record in records:
        product = record.get("productVO") or {}
        fetched = add_item.build_fetched_component(record, product)
        params = add_item.parse_search_params(record.get("paramLinkedMap") or {})
        candidate_resistance = first_param(params, ("阻值", "电阻值", "Resistance"))
        candidate_tolerance = first_param(params, ("精度", "容差", "Tolerance"))
        candidate_package = (
            add_item.sanitize_search_text(record.get("lightStandard"))
            or add_item.sanitize_search_text(product.get("encapsulationModel"))
        )
        if parse_resistance_to_milliohms(candidate_resistance) != resistance.ohms_milli:
            continue
        if normalize_package(candidate_package) != package_name:
            continue
        if parse_tolerance_basis_points(candidate_tolerance) != tolerance.basis_points:
            continue
        stock = int(product.get("stockNumber") or product.get("validStockNumber") or 0)
        price = first_price(product)
        candidates.append(
            ResistorMatch(
                part_number=fetched.part_number,
                resistance=resistance.display,
                resistance_key=resistance.ohms_milli,
                package_name=candidate_package,
                tolerance=candidate_tolerance,
                stock=stock,
                price=price,
                fetched=fetched,
            )
        )
    candidates.sort(key=lambda item: (item.stock <= 0, item.price is None, item.price or 0, item.part_number))
    return candidates[0] if candidates else None


def search_lcsc_products(keyword: str, timeout: float) -> list[dict[str, Any]]:
    session = requests.Session()
    session.trust_env = False
    session.headers.update(
        {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
            ),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Cache-Control": "no-cache",
            "Pragma": "no-cache",
            "Referer": "https://so.szlcsc.com/",
        }
    )
    url = "https://so.szlcsc.com/global.html?" + urlencode({"k": keyword})
    root = add_item.fetch_next_data(session, url, timeout)
    if root is None:
        return []
    return (
        root.get("props", {})
        .get("pageProps", {})
        .get("soData", {})
        .get("searchResult", {})
        .get("productRecordList", [])
    )


def first_param(params: dict[str, str], names: tuple[str, ...]) -> str | None:
    for name in names:
        for key, value in params.items():
            if key.strip().lower() == name.strip().lower():
                return value
    for name in names:
        for key, value in params.items():
            if name.strip().lower() in key.strip().lower():
                return value
    return None


def first_price(product: dict[str, Any]) -> float | None:
    prices = product.get("productPriceList") or []
    for price in prices:
        value = price.get("productPrice")
        if value is not None:
            return float(value)
    return None


def normalize_package(value: str | None) -> str:
    normalized = (value or "").strip().upper()
    if not normalized:
        raise add_item.WorkbookError("package is required")
    return normalized


def normalize_tolerance(value: str) -> NormalizedTolerance:
    basis_points = parse_tolerance_basis_points(value)
    if basis_points is None:
        raise add_item.WorkbookError(f"invalid resistor tolerance: {value}")
    display = value.strip()
    if not display.startswith("±"):
        display = f"±{display}"
    return NormalizedTolerance(display=display, basis_points=basis_points)


def parse_tolerance_basis_points(value: str | None) -> int | None:
    if not value:
        return None
    normalized = value.strip().replace("±", "").replace("+/-", "").replace("％", "%")
    match = re.search(r"([0-9]+(?:\.[0-9]+)?)\s*%", normalized)
    if not match:
        return None
    return int(round(float(match.group(1)) * 100))


def normalize_resistance(value: str) -> NormalizedResistance:
    ohms_milli = parse_resistance_to_milliohms(value)
    if ohms_milli is None:
        raise add_item.WorkbookError(f"invalid resistor value: {value}")
    return NormalizedResistance(display=format_resistance(ohms_milli), ohms_milli=ohms_milli)


def parse_resistance_to_milliohms(value: str | None) -> int | None:
    if not value:
        return None
    normalized = value.strip().replace("Ω", "").replace("ω", "").replace("欧", "")
    normalized = normalized.replace("Ｋ", "K").replace("ｋ", "K").replace("Ｍ", "M")
    normalized = normalized.upper().replace("OHM", "R").replace(" ", "")
    r_notation = re.fullmatch(r"([0-9]+(?:\.[0-9]+)?)R([0-9]*)", normalized)
    if r_notation:
        integer = r_notation.group(1)
        fraction = r_notation.group(2)
        value_ohms = float(f"{integer}.{fraction}") if fraction else float(integer)
        return int(round(value_ohms * 1000))
    match = re.fullmatch(r"([0-9]+(?:\.[0-9]+)?)([RKM]?)", normalized)
    if not match:
        return None
    number = float(match.group(1))
    suffix = match.group(2)
    multiplier = {"": 1, "R": 1, "K": 1_000, "M": 1_000_000}[suffix]
    return int(round(number * multiplier * 1000))


def format_resistance(ohms_milli: int) -> str:
    if ohms_milli % 1_000_000_000 == 0:
        return f"{ohms_milli // 1_000_000_000}MΩ"
    if ohms_milli >= 1_000_000_000:
        return f"{trim_number(ohms_milli / 1_000_000_000)}MΩ"
    if ohms_milli % 1_000_000 == 0:
        return f"{ohms_milli // 1_000_000}kΩ"
    if ohms_milli >= 1_000_000:
        return f"{trim_number(ohms_milli / 1_000_000)}kΩ"
    if ohms_milli % 1000 == 0:
        return f"{ohms_milli // 1000}Ω"
    return f"{trim_number(ohms_milli / 1000)}Ω"


def trim_number(value: float) -> str:
    return f"{value:.6g}"


def build_output_label(
    package_name: str,
    tolerance: NormalizedTolerance,
    values: list[NormalizedResistance],
) -> str:
    tolerance_label = re.sub(r"[^A-Za-z0-9.]+", "", tolerance.display)
    return f"resistors_{package_name}_{tolerance_label}_{len(values)}values"


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except add_item.WorkbookError as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(2)
