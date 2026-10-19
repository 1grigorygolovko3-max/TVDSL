# Отчёт HW1

## Regex (приоритет = порядок в `TokenType`)

| Токен | Regex |
|---|---|
| KW_FUNCTION | `function` |
| KW_REQUIRES | `requires` |
| KW_RETURNS | `returns` |
| KW_ENSURES | `ensures` |
| KW_USES | `uses` |
| KW_WHILE | `while` |
| KW_IF | `if` |
| KW_ELSE | `else` |
| KW_INVARIANT | `invariant` |
| KW_ASSERT | `assert` |
| KW_ASSUME | `assume` |
| KW_INT | `int` |
| KW_LENGTH | `length` |
| KW_OR/AND/NOT | `or` `and` `not` |
| KW_TRUE/FALSE | `true` `false` |
| KW_FORALL/EXISTS | `forall` `exists` |
| EQ_EQ/NOT_EQ/LESS_EQ/GREATER_EQ/ARROW | `==` `!=` `<=` `>=` `->` |
| DELIM | `(` `)` `[` `]` `{` `}` `,` `;` `:` |
| OP | `+` `-` `*` `/` `<` `>` `=` |
| COMMENT (skip) | `//` + `[^\n]*` |
| WS (skip) | `[ \t\r\n]+` |
| IDENT | `[A-Za-z_][A-Za-z0-9_]*` |
| INT | `0 \| [1-9][0-9]*` |

## Размеры DFA

- До минимизации: **130**
- После Хопкрофта: **120**

## Ловушка

Одно состояние с `accept = ERROR`, петля по всем ASCII `0..127`. В JSON: `"trap": <id>`, отсутствующие в `trans` символы ведут в trap (`unlistedGoesToTrap`).

`00`, `01`, `007` не принимаются как `INT` целиком.

Non-ASCII (`>127`) в таблицу не кладётся (массив `trans[128]`). В `Lexer.tokenize`: `if (c > 127)` → `ERROR`, разбор продолжается.

## Флаги

| Флаг | Действие |
|---|---|
| `--nfa` | печать NFA (`Start`, `End`) |
| `--dfa` | печать DFA (`Start`, `States`, `Accepting`) |
| `--min` | размер до/после минимизации |
| `--test` | встроенные тесты (~95) |
| `--test-file FILE` | блоки через `---`, каждый — отдельный тест |
| `--json PATH` | minDFA в JSON |
| `--help` | справка |


## Тесты

Формат `tests.txt`: блоки через `---`.
