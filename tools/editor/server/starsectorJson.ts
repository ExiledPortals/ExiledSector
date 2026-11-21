const BYTE_ORDER_MARK = String.fromCharCode(0xfeff);

function nextSignificantCharacter(text: string, from: number): string | undefined {
  for (let index = from; index < text.length; index++) {
    const character = text[index];
    if (character === '#') {
      while (index < text.length && text[index] !== '\n') {
        index++;
      }
    } else if (!/\s/.test(character)) {
      return character;
    }
  }
  return undefined;
}

export function strictJsonText(looseText: string): string {
  let strict = '';
  let inString = false;
  for (let index = 0; index < looseText.length; index++) {
    const character = looseText[index];
    if (inString) {
      strict += character;
      if (character === '\\') {
        strict += looseText[++index] ?? '';
      } else if (character === '"') {
        inString = false;
      }
    } else if (character === '"') {
      inString = true;
      strict += character;
    } else if (character === '#') {
      while (index + 1 < looseText.length && looseText[index + 1] !== '\n') {
        index++;
      }
    } else if (character === ',') {
      const following = nextSignificantCharacter(looseText, index + 1);
      if (following !== '}' && following !== ']') {
        strict += character;
      }
    } else {
      strict += character;
    }
  }
  return strict;
}

export function parseStarsectorJson(looseText: string): unknown {
  return JSON.parse(strictJsonText(looseText.startsWith(BYTE_ORDER_MARK) ? looseText.substring(1) : looseText));
}
