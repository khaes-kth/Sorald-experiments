#!/usr/bin/python3

# Clone https://github.com/SonarSource/sonar-java before running this script.

import glob
import json
import csv

PATH_TO_RULE_SPECS = "sonar-java/java-checks/src/main/resources/org/sonar/l10n/java/rules/java"
RULE_SPECS = glob.glob(f"{PATH_TO_RULE_SPECS}/*.json")

HANDLED_RULES = [
    "S1068",
    "S1118",
    "S1132",
    "S1155",
    "S1217",
    "S1444",
    "S1481",
    "S1596",
    "S1656",
    "S1854",
    "S1860",
    "S1948",
    "S2057",
    "S2095",
    "S2097",
    "S2111",
    "S2116",
    "S2142",
    "S2164",
    "S2167",
    "S2184",
    "S2204",
    "S2225",
    "S2272",
    "S2755",
    "S3032",
    "S3067",
    "S3984",
    "S4065",
    "S4973",
]

def does_sorald_repair_it(rule_key):
    if rule_key in HANDLED_RULES:
        return "YES"
    return "NO"

rows = []

for spec in RULE_SPECS:
    if "Sonar_way_profile.json" in spec:
        continue
    with open(spec) as f:
        spec_dict = json.loads(f.read())
        rule_key = spec_dict["sqKey"]
        url = f"https://rules.sonarsource.com/java/RSPEC-{rule_key[1:]}"
        try:
            quickfix = spec_dict["quickfix"]
            rows.append([
                rule_key,
                quickfix,
                does_sorald_repair_it(rule_key),
                url,
            ])
        except KeyError:
            rows.append([
                rule_key,
                "undefined",
                does_sorald_repair_it(rule_key),
                url,
            ])

with open('comparison.csv', 'w+') as f:
    rows.sort(key = lambda x: x[2], reverse=True)
    writer = csv.writer(f)
    writer.writerow(["Rule key", "SonarLint repair status", "Does Sorald repair it?", "Link"])
    writer.writerows(rows)

