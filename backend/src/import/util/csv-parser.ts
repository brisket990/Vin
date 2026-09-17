/**
 * Minimal RFC4180-ish CSV parser matching ExportService's `csvEscape`: fields
 * are comma-separated, and a field containing a comma, quote or newline is
 * wrapped in double quotes with internal quotes doubled (`"` -> `""`). This
 * is intentionally hand-rolled rather than pulling in a dependency -- the
 * grammar it needs to round-trip is exactly the one `csvEscape` produces.
 *
 * Handles `\r\n` and bare `\n` line endings, a trailing newline (or not),
 * and drops a lone blank line at the end of the file.
 */
export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let inQuotes = false;
  let i = 0;
  const len = text.length;

  const pushField = () => {
    row.push(field);
    field = '';
  };
  const pushRow = () => {
    pushField();
    rows.push(row);
    row = [];
  };

  while (i < len) {
    const char = text[i];

    if (inQuotes) {
      if (char === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i += 2;
          continue;
        }
        inQuotes = false;
        i++;
        continue;
      }
      field += char;
      i++;
      continue;
    }

    if (char === '"') {
      inQuotes = true;
      i++;
      continue;
    }
    if (char === ',') {
      pushField();
      i++;
      continue;
    }
    if (char === '\r') {
      i++;
      continue;
    }
    if (char === '\n') {
      pushRow();
      i++;
      continue;
    }
    field += char;
    i++;
  }

  // Final field/row, for a file that doesn't end with a newline.
  if (field.length > 0 || row.length > 0) {
    pushRow();
  }

  // Drop a lone trailing blank line (e.g. the file ends with "\n").
  return rows.filter((r) => !(r.length === 1 && r[0] === ''));
}
