const NUMBER_TEXT = /^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?$/;
const PLAIN_DECIMAL_TEXT = /^(-?)(\d*)(?:\.(\d*))?$/;

export function isNumberText(text: unknown): boolean {
  return NUMBER_TEXT.test(String(text).trim());
}

function stripLeadingZeros(digits: string): string {
  let start = 0;
  while (start < digits.length - 1 && digits[start] === '0') {
    start++;
  }
  return digits.slice(start);
}

function stripTrailingZeros(digits: string): string {
  let end = digits.length;
  while (end > 0 && digits[end - 1] === '0') {
    end--;
  }
  return digits.slice(0, end);
}

export function shiftDecimalText(text: string, places: number): string {
  const trimmed = String(text).trim();
  const match = PLAIN_DECIMAL_TEXT.exec(trimmed);
  if (!match || match[2] + (match[3] ?? '') === '') {
    const number = Number.parseFloat(text);
    return Number.isNaN(number) ? trimmed : String(number * Math.pow(10, places));
  }
  let digits = match[2] + (match[3] ?? '');
  let point = match[2].length + places;
  while (point < 0) {
    digits = '0' + digits;
    point++;
  }
  while (digits.length < point) {
    digits += '0';
  }
  const whole = stripLeadingZeros(digits.substring(0, point)) || '0';
  const fraction = stripTrailingZeros(digits.substring(point));
  return (match[1] && /[1-9]/.test(digits) ? '-' : '') + whole + (fraction ? '.' + fraction : '');
}

export function fractionToPercentText(fraction: string): string {
  return fraction === '' ? '' : shiftDecimalText(fraction, 2);
}

export function percentToFractionText(percent: string): string {
  return String(percent).trim() === '' ? '' : shiftDecimalText(percent, -2);
}

export function formatPercent(value: number | null | undefined): string {
  return value === null || value === undefined || Number.isNaN(value) ? '' : String(Number(value.toFixed(2)));
}
