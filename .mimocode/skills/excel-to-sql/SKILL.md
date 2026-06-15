---
name: excel-to-sql
description: Convert Excel files to SQL INSERT statements. Read Excel, parse data, map to DB models, generate SQL.
---

# Excel to SQL Conversion

Convert Excel spreadsheets to MySQL INSERT statements for database import.

## When to Use

- User provides an Excel file (.xlsx) and asks to convert it to SQL
- User needs to import spreadsheet data into MySQL database
- Data needs to be transformed before insertion (e.g., ID generation, foreign key mapping)

## Workflow

### 1. Read and Parse Excel

```powershell
# Use PowerShell with COM Excel object (Windows)
$excel = New-Object -ComObject Excel.Application
$excel.Visible = $false
$excel.DisplayAlerts = $false
try {
    $workbook = $excel.Workbooks.Open("path\to\file.xlsx")
    # Iterate sheets, extract headers and data
} finally {
    $excel.Quit()
    [System.Runtime.InteropServices.Marshal]::ReleaseComObject($excel) | Out-Null
}
```

### 2. Analyze Data Structure

- Identify column headers and data types
- Check for foreign key relationships
- Detect required fields and constraints
- Note any data cleaning needed (trimming, format conversion)

### 3. Map to Database Models

- Read existing Prisma schema or database models
- Match Excel columns to database fields
- Generate IDs (snowflake, UUID, or sequential based on project convention)
- Handle foreign key dependencies (create parent records first)

### 4. Generate SQL

```sql
-- Example output format
INSERT INTO `table_name` (`id`, `field1`, `field2`, `created_at`) VALUES
(123456, 'value1', 'value2', NOW()),
(123457, 'value3', 'value4', NOW());
```

### 5. Output Files

- Save SQL to a specified directory (default: `doc/` or current directory)
- If multiple tables, create separate files or one combined file
- Include comments explaining data transformations

## Project Conventions

- **ID Generation**: Use project's ID generator (snowflake, UUID, etc.)
- **Timestamps**: Use `NOW()` or specific timestamp as required
- **Encoding**: UTF-8 for Chinese characters
- **File Location**: Check `doc/` directory for existing SQL files

## Common Patterns

### Tenant/Resident Data Import

1. Parse Excel with building/floor/room hierarchy
2. Generate building records → floor records → room records → tenant records
3. Handle phone number masking for privacy
4. Create user accounts if needed

### Financial Data Import

1. Parse Excel with transaction records
2. Map to appropriate accounting tables
3. Handle date format conversions
4. Validate numeric precision

## Error Handling

- Log rows that fail validation
- Generate error report alongside SQL output
- Provide summary of successful/failed conversions
