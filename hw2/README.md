# HW2. Парсер Funny

LL(1)-парсер рекурсивным спуском поверх лексера HW1.
Лексер по-прежнему строится как НКА Томпсона, ДКА и минимальный ДКА.
У токенов есть позиция `строка:столбец`. Парсер печатает AST и диагностику.

Грамматика и список отклонений от приложения В: [grammar.txt](grammar.txt).
Краткий отчёт: [REPORT.md](REPORT.md).

## Сборка

Нужны JDK 21 и Maven.

```bash
#(предварительно следует загрузить snapshot hw1 в кэш Maven: mvn clean install в директории hw1)#
mvn clean package
```

JUnit подключён только со scope `test`. В `Main` его вызывать не нужно: `mvn test` сам запускает `lexer.FunnyLexerTest` и `parser.ParserTest`.

## Запуск

Разобрать файл и напечатать AST. Код возврата 0, если ошибок нет, иначе 1.
Сообщения об ошибках идут в тот же вывод, каждая строка начинается с `error`.

```bash
java -jar target/hw2-1.0-SNAPSHOT.jar --parse examples/inc.funny
java -jar target/hw2-1.0-SNAPSHOT.jar --parse examples/broken.funny
printf 'inc(a:int) returns r:int { r = a + 1; }\n' | java -jar target/hw2-1.0-SNAPSHOT.jar --parse
```

Лексер по-прежнему доступен:

```bash
java -jar target/hw2-1.0-SNAPSHOT.jar --min
java -jar target/hw2-1.0-SNAPSHOT.jar --test
```

`--json` записывает таблицу ДКА. Без флагов запускается демонстрация лексера, как в HW1.

## AST

Текстовое дерево с отступом в два пробела. Узел `Assign` для `a[i] = v` уже содержит вызов `set(a, i, v)`. Это дерево можно обойти по классам пакета `ast` на следующих домашних работах.

Пример:

```
Module
  Function inc
    params
      Param a : int
    returns
      Param r : int
    body
      Block
        Assign r
          Binary +
            Var a
            Int 1
```

Ошибка содержит позицию: `error 1:11: ожидалось ')', найдено 'returns'`.

## Тесты

Позитивные: снимок небольшой функции, приоритет `+` `-` `*` и унарного `-`, `requires`/`ensures`/`uses`, `if`/`else`, `while` с `invariant`, `assert`/`assume`, `int[]`, чтение `a[i]` и запись как `set`, кортеж `x, y = id(...)`, правоассоциативное `->`.
Негативные: пустой ввод, незакрытая скобка, лишний токен, неверный оператор, лексическая ошибка. После сломанной функции паника дочитывает вход и разбирает следующую, не зацикливаясь.
