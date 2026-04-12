#!/bin/bash


echo "Starting database import process"

cd "$(dirname "$0")"

pushd ../../.data/

echo "Importing tainted database schema..."
docker exec -i xss-demo-mysql mysql -uroot -proot xss_demo < "tainted_db_dump.sql"
if [ $? -eq 0 ]; then
    echo "Database import completed successfully"
else
    echo "ERROR: Database import failed"
    exit 1
fi

popd

# Check if taint columns exist in countries table
echo "Verifying taint columns in countries table..."
TAINT_COLS=$(docker exec -i xss-demo-mysql mysql -uroot -proot xss_demo -N -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='countries' AND COLUMN_NAME IN ('__taint__name','__taint__id');")

if [ "$TAINT_COLS" -ne 2 ]; then
    echo "ERROR: Taint columns missing from countries table"
    exit 1
fi

echo "Successfully verified taint columns"
echo "Database import process completed successfully"

