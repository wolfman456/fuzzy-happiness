# Learning

A communal place to learn the basics of the tech this project runs on. Work through it at your own pace — a little bit every day beats a big pile once a week.

When you get stuck, the order is:

1. Re-read the docs / the error message.
2. Search the exact words of the error.
3. Ask the group.

There are no rankings and no shame here — the point is to get everyone comfortable enough to ship real code to this repo.

---

## How this project fits together

One mental model to keep in your head while learning:

| Layer     | Tech                          | Folder          | Job                                  |
|-----------|-------------------------------|-----------------|---------------------------------------|
| Frontend  | JavaScript + React            | `tabletopweb/`  | Runs in the browser — what you see    |
| Backend   | Java + Spring Boot            | `tabletopserv/` | Rules, dice, sessions — the "brain"  |
| Gateway   | JavaScript + Node + Express   | `tabletopgateway/` | The only door to the internet (SRD) |

Data flows one way for external calls: browser (JS) → backend (Java), then backend → gateway (Node) when it needs internet data. The backend never talks to the internet directly.

---

## Java

**Why it matters here:** the backend (rules, dice, sessions, monsters) is Java 21. Java is statically typed, so the compiler catches a lot of mistakes before you ever run the code.

### Core concepts to learn

- Variables and primitive types (`int`, `double`, `boolean`, `String`)
- Methods: input **parameters**, a return type, and a body
- Control flow: `if/else`, `for`, `while`, `switch`
- Arrays and collections (`List`, `Map`)
- Classes and objects: fields, constructors, methods
- OOP basics: inheritance, interfaces, encapsulation
- Exceptions: `try/catch`
- Build and test: Maven (`./mvnw test`) + JUnit tests

### Resources

| Link | What it is | Best for |
|------|------------|----------|
| [Java 101: Zero to Hero](https://dev.to/louaiboumediene/java-101-zero-to-hero-course-4jna) (dev.to) | Free 20-part beginner series | Read-first overview |
| [CodingBat Java](https://codingbat.com/java) | Tiny drill exercises, instant feedback | Daily practice — start here |
| [W3Resource Java Exercises](https://www.w3resource.com/java-exercises/) | Hundreds of graded exercises | Step up from CodingBat |
| [GeeksforGeeks Java](https://www.geeksforgeeks.org/java/java-exercises/) | Problems plus explanations | When you want the "why" |
| [HackerRank Java](https://www.hackerrank.com/) | In-browser challenges | Confidence + interview practice |
| [Oracle's Java Tutorials](https://docs.oracle.com/javase/tutorial/) | Official docs | Looking up "how do I…" |

### Beginner exercises

1. **FizzBuzz.** For 1 to 100: print `Fizz` if divisible by 3, `Buzz` if by 5, `FizzBuzz` if by both, otherwise the number.
2. **Sum of digits.** `sumDigits(1234)` returns `10`.
3. **Temperature.** Write `celsiusToFahrenheit(c)` using `c * 9 / 5 + 32`.
4. **Count vowels.** Count how many vowels (`a e i o u`) a string contains.
5. **OOP.** Write a tiny `Monster` class with a `name` field, a `hitDice` field, and a `describe()` method. Make two monsters in `main()` and describe them — you are re-implementing the real `Monster` model in `tabletopserv`.

<details>
<summary>Model answers (try first, then peek)</summary>

1. FizzBuzz<br>

```java
for (int i = 1; i <= 100; i++) {
    if (i % 15 == 0)      System.out.println("FizzBuzz");
    else if (i % 3 == 0)  System.out.println("Fizz");
    else if (i % 5 == 0)  System.out.println("Buzz");
    else                  System.out.println(i);
}
```

2. Sum of digits<br>

```java
int sumDigits(int n) {
    int sum = 0;
    while (n > 0) {
        sum += n % 10;
        n /= 10;
    }
    return sum;
}
```

3. Temperature<br>

```java
double celsiusToFahrenheit(double c) {
    return c * 9 / 5 + 32;
}
```

4. Count vowels<br>

```java
int countVowels(String s) {
    int count = 0;
    for (char c : s.toLowerCase().toCharArray()) {
        if ("aeiou".indexOf(c) >= 0) count++;
    }
    return count;
}
```

5. Monster class<br>

```java
class Monster {
    String name;
    int hitDice;

    Monster(String name, int hitDice) {
        this.name = name;
        this.hitDice = hitDice;
    }

    void describe() {
        System.out.println(name + " has " + hitDice + " Hit Dice");
    }

    public static void main(String[] args) {
        Monster goblin = new Monster("Goblin", 2);
        Monster bugbear = new Monster("Bugbear", 5);
        goblin.describe();
        bugbear.describe();
    }
}
```

</details>

---

## JavaScript

**Why it matters here:** the frontend (`tabletopweb`) is React — all JavaScript — and the gateway (`tabletopgateway`) is Node, also JavaScript. It is the one language used on both sides of the app.

### Core concepts to learn

- Variables: `const` and `let`
- Functions and arrow functions
- Arrays and objects and their friends: `map`, `filter`, `find`
- Template literals: `` `Hello ${name}` ``
- The DOM and click events
- `fetch()` + `async`/`await` (talking to the backend)
- ES modules: `import` / `export`

### Resources

| Link | What it is | Best for |
|------|------------|----------|
| [MDN: JavaScript Guide](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide) (Mozilla) | Official, thorough docs + tutorial | Reference and concepts |
| [JavaScript.info](https://javascript.info/) | Modern, well-explained course | Your main learning path |
| [freeCodeCamp](https://www.freecodecamp.org/) | Free interactive curriculum + certificates | Guided track |
| [Exercism: JavaScript](https://exercism.org/tracks/javascript) | Real exercises with a mentor | Practice |
| [Codewars](https://www.codewars.com/) | Gamified challenges ("kata") | Once you can walk |

### Beginner exercises

1. **Reverse a string.** `"hello"` → `"olleh"` (without `.reverse()` first).
2. **FizzBuzz in JS.** Same rules as the Java one.
3. **Map & filter.** From `[3, 15, 7, 22, 9, 100]`, keep numbers greater than 10 and double them.
4. **Async fetch.** Fetch <https://jsonplaceholder.typicode.com/todos> and render the titles as a list (this is `fetch` + the DOM together).
5. **Click counter.** A page with one button that shows how many times it was clicked.

<details>
<summary>Model answers (try first, then peek)</summary>

1. Reverse a string<br>

```js
function reverse(s) {
    let out = "";
    for (let i = s.length - 1; i >= 0; i--) out += s[i];
    return out;
}
```

2. FizzBuzz<br>

```js
for (let i = 1; i <= 100; i++) {
    if (i % 15 === 0)      console.log("FizzBuzz");
    else if (i % 3 === 0)  console.log("Fizz");
    else if (i % 5 === 0)  console.log("Buzz");
    else                   console.log(i);
}
```

3. Map & filter<br>

```js
const nums = [3, 15, 7, 22, 9, 100];
const result = nums.filter(n => n > 10).map(n => n * 2);
```

4. Async fetch<br>

```js
const res = await fetch("https://jsonplaceholder.typicode.com/todos");
const todos = await res.json();
for (const todo of todos) {
    const li = document.createElement("li");
    li.textContent = todo.title;
    document.body.appendChild(li);
}
```

5. Click counter<br>

```html
<button id="btn">Click me</button>
<p id="count">0</p>
```

```js
let count = 0;
document.getElementById("btn").addEventListener("click", () => {
    count++;
    document.getElementById("count").textContent = count;
});
```

</details>

---

## SQL

**Why it matters here:** the game will eventually run on PostgreSQL. In this project you will rarely write raw SQL — Spring Data (JPA) turns Java objects into SQL for you — but understanding tables and queries makes the whole data model make sense.

### Core concepts to learn

- Tables, rows, columns; primary and foreign keys
- `SELECT`, `WHERE`, `ORDER BY`, `LIMIT`
- `INSERT`, `UPDATE`, `DELETE`
- Aggregates: `COUNT`, `SUM`, `GROUP BY`
- Joins: inner and left

### Resources

| Link | What it is | Best for |
|------|------------|----------|
| [SQLBolt](https://sqlbolt.com/) | Interactive lessons in the browser | Best first step |
| [PostgreSQL Exercises](https://pgexercises.com/) | Real database, real questions | Practice |
| [PostgreSQL Tutorial](https://www.postgresqltutorial.com/) | Structured course | Deeper understanding |
| [W3Schools SQL](https://www.w3schools.com/sql/) | Quick reference with live "Try it" | Cheat sheet |

### Exercise — the monsters table (ties into the real game)

Create this table and answer the questions below:

```sql
CREATE TABLE monster (
    id          INTEGER PRIMARY KEY,
    name        TEXT,
    cr          REAL,        -- challenge rating
    role        TEXT,        -- combat role
    speed_feet  INTEGER
);

INSERT INTO monster VALUES
    (1, 'Kobold',           0.125, 'minion',     30),
    (2, 'Goblin',           0.25,  'minion',     30),
    (3, 'Bugbear',          1,     'bruiser',    30),
    (4, 'Owlbear',          3,     'bruiser',    40),
    (5, 'Hill Giant',       5,     'bruiser',    40),
    (6, 'Young Red Dragon', 10,    'controller', 40),
    (7, 'Goblin Shaman',    2,     'controller', 30),
    (8, 'Hobgoblin Archer', 1,     'sniper',     30);
```

1. Find every monster with `cr >= 5`.
2. Count how many monsters exist per `role`.
3. List the 3 fastest monsters (highest `speed_feet` first).
4. Change the Kobold's `name` to "Kobold, Veteran" with `UPDATE`.

<details>
<summary>Model answers (try first, then peek)</summary>

1.<br>

```sql
SELECT * FROM monster WHERE cr >= 5;
```

2.<br>

```sql
SELECT role, COUNT(*) FROM monster GROUP BY role;
```

3.<br>

```sql
SELECT * FROM monster ORDER BY speed_feet DESC LIMIT 3;
```

4.<br>

```sql
UPDATE monster SET name = 'Kobold, Veteran' WHERE id = 1;
```

</details>

---

## Git & GitHub (the minimum)

Every change to the repo travels the same path:

**clone → branch → edit → commit → push → pull request**

Work through these in order:

```sh
# 1. Get the code onto your machine
git clone git@github.com:wolfman456/fuzzy-happiness.git

# 2. Make your own branch (never commit to develop)
git checkout -b feature/your-idea

# 3. Edit files (e.g. with your favourite editor)

# 4. Stage and snapshot your change
git add <file-that-changed>
git commit -m "short summary of what and why"

# 5. Upload your branch and open a Pull Request on GitHub
git push -u origin feature/your-idea
```

**Golden rules:**

- Never commit directly to `develop` — always work on a branch and open a PR.
- Commit messages read like a changelog: a one-line summary, then why.
- Run `git pull --ff-only` before you start and after a PR is merged.
- Branch names look like `feature/<short-slug>`.

### Resources

| Link | What it is |
|------|------------|
| [GitHub: Hello World](https://docs.github.com/en/get-started/start-your-journey/hello-world) | GitHub's own 15-minute intro |
| [Learn Git Branching](https://learngitbranching.js.org/) | Visual + interactive — the best way to "get" branches |
| [Oh Shit, Git!?!](https://ohshitgit.com/) | For when you break something |

### Cheat sheet

| I want to… | Command |
|---|---|
| See what changed | `git status` · `git diff` |
| Stage a file | `git add <file>` |
| Save a snapshot | `git commit -m "…"` |
| Upload a branch | `git push -u origin <branch>` |
| Get the latest | `git pull --ff-only` |
| Create a branch | `git checkout -b <name>` |
| Switch branches | `git checkout <name>` |
| Browse history | `git log --oneline` |

### Exercise — do the whole loop for real

On this repo: create `feature/git-practice`, add a short note under a new `## What I learned` heading at the bottom of this file saying what *you* got from this section, then commit, push, and open a pull request. That is the exact workflow we use for every change.

---

## Daily challenge

Same rules as before: **first to get it right doesn't get made fun of.** Try it on your own first — the model answer is folded away below.

### Challenge: counting commas

You are given an integer `n`.

Return the total number of commas used when writing all integers from `1` to `n` (inclusive) in standard number formatting.

In standard formatting:

- A comma is inserted after every three digits from the right.
- Numbers with fewer than four digits contain no commas.

**Example 1**

```
Input:  n = 1002
Output: 3
```

The numbers `1,000`, `1,001`, and `1,002` each contain one comma → `3` total.

**Example 2**

```
Input:  n = 998
Output: 0
```

All numbers from 1 to 998 have fewer than four digits, so no commas are used.

**Constraints**

- `1 <= n <= 100_000`

### Setup block

```java
class Solution {
    public int countCommas(int n) {
        // your code here
    }
}
```

<details>
<summary>Model answer + reasoning (try it first!)</summary>

A number with `d` digits uses one comma for every full group of three digits after the first — that is `(d - 1) / 3` commas (integer division). So counting from 1 to `n`:

```java
class Solution {
    public int countCommas(int n) {
        int total = 0;
        for (int x = 1; x <= n; x++) {
            int digits = String.valueOf(x).length();
            total += (digits - 1) / 3;
        }
        return total;
    }
}
```

**Bonus (fast path):** instead of looping, count by digit ranges. Every 4–6 digit number adds exactly 1 comma, every 7–9 digit number adds 2, and so on:

```java
class Solution {
    public int countCommas(int n) {
        long total = 0;
        long start = 1, digits = 1;
        while (start <= n) {
            long end = Math.min(n, start * 1000 - 1);
            long count = end - start + 1;
            total += count * ((digits - 1) / 3);
            start *= 1000;
            digits += 3;
        }
        return (int) total;
    }
}
```

</details>

---

### Challenge backlog (pick one for next time)

- **Dice histogram:** roll `2d6` a hundred times and print which total (2–12) comes up most — a miniature version of the real dice engine.
- **Palindrome check:** is the string the same forwards and backwards? (e.g. `"racecar"` → yes).
- **Initials:** turn `"Ada Lovelace"` into `"AL"`.

---

## Tips that actually work

- Five minutes a day beats three hours once a week.
- Type every example out by hand — no copy-paste.
- Try to explain your answer to the group *before* you check it.
- Read the full error message, then search its exact wording, then ask.
- This file is a living document: if a link is broken or a step is confusing, fix it in a pull request.