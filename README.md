# IFT3913 – Tâche 2 : augmentation d'une suite de tests avec ChatUniTest et pitest

| Nom complet | Identifiant GitHub |
|---|---|
| Yanis Boukhili | yns-bkh |

Travail réalisé seul (aucun coéquipier disponible). Branche : `tache2`. Le README d'origine d'Apache Tika est conservé dans [README-TIKA.md](README-TIKA.md).

## Sommaire

1. [Résumé des résultats](#1-résumé-des-résultats)
2. [Reproduire](#2-reproduire)
3. [Classes choisies et justification](#3-classes-choisies-et-justification)
4. [ChatUniTest dans le pipeline Maven](#4-chatunitest-dans-le-pipeline-maven)
5. [Résultat de la génération](#5-résultat-de-la-génération)
6. [Critique des oracles générés vs. écrits à la main](#6-critique-des-oracles-générés-vs-écrits-à-la-main)
7. [pitest : configuration](#7-pitest--configuration)
8. [Mutation : tests originaux vs. tests générés](#8-mutation--tests-originaux-vs-tests-générés)
9. [Tests ajoutés à la main](#9-tests-ajoutés-à-la-main)
10. [Mutants restants (équivalents)](#10-mutants-restants-équivalents)
11. [GitHub Action](#11-github-action)
12. [Déclaration d'utilisation de l'IA](#12-déclaration-dutilisation-de-lia)

---

## 1. Résumé des résultats

Module : `tika-core`. Classes : `org.apache.tika.io.LookaheadInputStream` et `org.apache.tika.utils.CharsetUtils`.

| Suite de tests | LookaheadInputStream | CharsetUtils | Total |
|---|---|---|---|
| Tests originaux | 19/33 (58 %) | 15/25 (60 %) | 34/58 (59 %) |
| + tests générés par ChatUniTest (corrigés) | 25/33 (76 %) | 15/25 (60 %) | 40/58 (69 %) |
| + tests écrits à la main | **33/33 (100 %)** | **23/25 (92 %)** | **56/58 (97 %)** |

Couverture de lignes des deux classes : 79 % → 86 % → 92 %. Les 2 mutants restants sont équivalents (section 10).

ChatUniTest (avec `qwen2.5-coder:7b` exécuté localement par Ollama) a fait 21 tentatives de génération. Seules **2 ont produit un test qui compile et passe sans intervention (9,5 %)**. 5 autres ont été récupérées et corrigées à la main (section 5).

Rapports pitest HTML complets :
- [tache2-rapports/pit-baseline/](tache2-rapports/pit-baseline/index.html) : tests originaux
- [tache2-rapports/pit-generes/](tache2-rapports/pit-generes/index.html) : originaux + générés
- [tache2-rapports/pit-final/](tache2-rapports/pit-final/index.html) : originaux + générés + manuels

## 2. Reproduire

Prérequis : JDK 21. Utiliser le wrapper `./mvnw` (Maven 3.9), car le `.mvn/maven.config` de Tika contient des commentaires que Maven 3.8 refuse.

```bash
# 1. construire tika-core et le pom parent
./mvnw -pl tika-core -am install -DskipTests -Dcheckstyle.skip -Drat.skip -Dforbiddenapis.skip

# 2. exécuter les tests des deux classes (originaux + générés + manuels)
cd tika-core
../mvnw test -Dcheckstyle.skip -Dtest='LookaheadInputStream*Test,CharsetUtils*Test' -Dsurefire.failIfNoSpecifiedTests=false

# 3. analyse de mutation (classes et tests ciblés définis dans tika-core/pom.xml)
../mvnw org.pitest:pitest-maven:mutationCoverage -Dcheckstyle.skip
# rapport : tika-core/target/pit-reports/index.html
```

Pour régénérer les tests avec ChatUniTest (nécessite Ollama, voir section 4) :

```bash
ollama pull qwen2.5-coder:7b
ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat
OLLAMA_CONTEXT_LENGTH=8192 ollama serve &
cd tika-core
../mvnw io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class -DselectClass=org.apache.tika.io.LookaheadInputStream -Dcheckstyle.skip
../mvnw io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class -DselectClass=org.apache.tika.utils.CharsetUtils -Dcheckstyle.skip
```

Remarque : `-Dcheckstyle.skip` est nécessaire, car les tests générés (imports `*`, pas d'en-tête de licence) ne respectent pas le checkstyle de Tika. Les tests générés sont conservés tels quels, à leurs corrections près, pour rester fidèles à la sortie de l'outil.

## 3. Classes choisies et justification

J'ai d'abord exécuté pitest avec **tous** les tests de tika-core sur 6 classes candidates qui ont déjà une classe de test :

| Classe | Mutants | Tués | Survivants | Sans couverture | Score |
|---|---|---|---|---|---|
| io.FilenameUtils | 115 | 69 | 30 | 16 | 60 % |
| mime.MediaType | 85 | 60 | 10 | 14 | 70 % |
| **io.LookaheadInputStream** | 33 | 19 | 5 | 9 | **58 %** |
| **utils.CharsetUtils** | 25 | 15 | 5 | 5 | **60 %** |
| detect.TextDetector | 14 | 10 | 3 | 0 | 71 % |
| utils.RegexUtils | 4 | 3 | 1 | 0 | 75 % |

J'ai retenu les deux classes les plus faibles de taille raisonnable. Elles ont déjà des tests (`LookaheadInputStreamTest`, `CharsetUtilsTest`), mais ceux-ci ne couvrent pas tout le code et laissent des mutants vivants. Avec seulement leurs propres tests (configuration retenue pour la suite) :

**LookaheadInputStream** (19/33, 58 %, couverture de lignes incomplète) :
- `read(byte[], int, int)` (lignes 103-110) et `markSupported()` (ligne 129) ne sont **jamais exécutées** : 9 mutants `NO_COVERAGE`.
- 5 mutants vivants dans le code couvert :
  - L67/L68 : `stream.mark(n)` peut être retiré du constructeur sans qu'aucun test ne le remarque ;
  - L81 : borne `buffered < buffer.length` ;
  - L82 : `buffer.length - buffered` ;
  - L86 : appel à `close()` quand le flux est épuisé.

**CharsetUtils** (15/25, 60 %) :
- Les branches ICU4J de `isSupported` (L107, L108, L118) et de `forName` (L183-L187) ne sont jamais exécutées, car ICU4J est absent du classpath de tika-core : 5 mutants `NO_COVERAGE`, plus des survivants.
- L159 : le rejet de `"none"`/`"no"` peut être retiré, car `clean()` renvoie `null` dans les deux cas.
- L157/L178 : vérifications `charset != null`.

## 4. ChatUniTest dans le pipeline Maven

ChatUniTest est intégré dans [tika-core/pom.xml](tika-core/pom.xml) de deux façons :

1. **Plugin** `io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1`, configuré pour un LLM local :

```xml
<plugin>
  <groupId>io.github.zju-aces-ise</groupId>
  <artifactId>chatunitest-maven-plugin</artifactId>
  <version>2.1.1</version>
  <configuration>
    <apiKeys>ollama</apiKeys>                    <!-- Ollama n'exige pas de clé -->
    <model>codeqwen:v1.5-chat</model>            <!-- alias Ollama de qwen2.5-coder:7b -->
    <url>http://localhost:11434/v1/chat/completions</url>
    <testNumber>2</testNumber>                   <!-- 2 tentatives par méthode -->
    <maxRounds>3</maxRounds>                     <!-- 3 rondes de réparation par tentative -->
    <maxPromptTokens>6000</maxPromptTokens>
    <temperature>0.2</temperature>
    <thread>false</thread>
    <stopWhenSuccess>true</stopWhenSuccess>
    <testOutput>${project.basedir}/chatunitest-tests</testOutput>
  </configuration>
</plugin>
```

2. **Dépendance** `io.github.ZJU-ACES-ISE:chatunitest-starter:1.5.0` (scope `test`), qui fournit Mockito et JUnit aux tests générés. J'en exclus `junit-vintage-engine` et `junit-platform-runner`, incompatibles avec JUnit 6 utilisé par Tika. Les autres versions sont alignées par le `junit-bom` du parent.

**Modèle local.** Ollama 0.40.3, installé sans droits administrateur, avec le modèle `qwen2.5-coder:7b` (quantifié Q4, 4,7 Go) sur un GPU RTX 4050 de 6 Go.

Trois problèmes rencontrés et leur solution :
- ChatUniTest 2.1.1 n'accepte qu'une liste fermée de noms de modèles (`gpt-*`, `code-llama`, `codeqwen:v1.5-chat`). Le nom `qwen2.5-coder:7b` donne `No Model with name qwen2.5-coder:7b`. Solution : créer un alias Ollama avec `ollama cp qwen2.5-coder:7b codeqwen:v1.5-chat`.
- ChatUniTest construit des prompts jusqu'à ~11 000 tokens, alors qu'Ollama tronque par défaut à 4096 tokens avec 6 Go de VRAM. Solution : `OLLAMA_CONTEXT_LENGTH=8192` et `maxPromptTokens=6000`.
- La 2e tentative pour `CharsetUtils.forName` a été interrompue à sa dernière ronde : le modèle était passé sur CPU (1,4 token/s) faute de RAM.

## 5. Résultat de la génération

### Où sont les tests générés ?

- Sortie brute du plugin (tests ayant passé la validation de ChatUniTest) : [tika-core/chatunitest-tests/tika-parent/tika-core/](tika-core/chatunitest-tests/tika-parent/tika-core/).
- Toutes les tentatives échouées, avec le code de chaque ronde et l'erreur de compilation ou d'exécution : [tache2-rapports/chatunitest-brut/tentatives-echouees/](tache2-rapports/chatunitest-brut/tentatives-echouees/). Logs complets : [log-LookaheadInputStream.txt](tache2-rapports/chatunitest-brut/log-LookaheadInputStream.txt), [log-CharsetUtils.txt](tache2-rapports/chatunitest-brut/log-CharsetUtils.txt).
- Tests intégrés à la suite (dans `tika-core/src/test/java`) : les fichiers `*_N_M_Test.java`, nommés comme ChatUniTest les a nommés.

### Compilent-ils et s'exécutent-ils sans intervention ?

Non, dans l'immense majorité des cas. Résultat de chaque tentative (2 par méthode, 3 rondes de réparation automatique chacune) :

| Méthode | Tentative 0 | Tentative 1 |
|---|---|---|
| LookaheadInputStream.close | échec compilation | échec compilation |
| LookaheadInputStream.read() | échec exécution | échec compilation |
| LookaheadInputStream.read(byte[],int,int) | échec exécution | échec exécution |
| LookaheadInputStream.skip | échec compilation | échec exécution |
| LookaheadInputStream.available | échec compilation | échec compilation |
| LookaheadInputStream.markSupported | **réussi (ronde 0)** | – |
| LookaheadInputStream.mark | échec compilation | échec compilation |
| LookaheadInputStream.reset | échec compilation | échec compilation |
| CharsetUtils.isSupported | échec exécution | **réussi (ronde 0)** |
| CharsetUtils.clean | échec exécution | échec compilation |
| CharsetUtils.forName | échec compilation | échec compilation (interrompu) |

Bilan : 21 tentatives. **2 réussies**, 13 échecs de compilation, 6 échecs d'exécution. Les réparations automatiques n'ont jamais réussi : le modèle répète la même erreur à chaque ronde.

Causes des **erreurs de compilation** (dernière ronde) :
- `when(stream.reset())` sur une méthode `void` (close, reset) : `'void' type not allowed here` ;
- accès à des champs privés : `buffered`, `position`, `COMMON_CHARSETS` (available, clean, forName) ;
- méthodes ou classes inventées : `cannot find symbol` (available, mark, read, skip, forName) ;
- réflexion sans déclarer `NoSuchFieldException` (mark, reset).

Causes des **échecs d'exécution** : oracles faux, détaillés à la section 6.

### Corrections nécessaires

J'ai intégré les 2 tests réussis tels quels. J'ai aussi récupéré la dernière ronde de 5 tentatives échouées qui visaient les méthodes les moins testées, puis je les ai corrigées **au minimum**. Chaque correction est marquée `// CORRECTION n` dans le code.

| Test intégré | Origine | Corrections | Nature |
|---|---|---|---|
| [LookaheadInputStream_markSupported_6_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_markSupported_6_0_Test.java) | réussi | 0 | – |
| [CharsetUtils_isSupported_1_1_Test](tika-core/src/test/java/org/apache/tika/utils/CharsetUtils_isSupported_1_1_Test.java) | réussi | 0 | – |
| [LookaheadInputStream_read_3_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_read_3_0_Test.java) | échec exécution | 1 (appliquée à 8 lignes) | oracle `verify(read(buffer, off, len))` remplacé par `verify(read(any(), eq(0), eq(10)))` |
| [LookaheadInputStream_read_2_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_read_2_0_Test.java) | échec exécution | 3 | Mockito `LENIENT` (stubs inutiles), 2 oracles faux |
| [LookaheadInputStream_skip_4_1_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_skip_4_1_Test.java) | échec exécution | 3 | Mockito `LENIENT`, 3 `verify(available())` supprimés, 1 test `@Disabled` (voir 6.3) |
| [LookaheadInputStream_close_0_0_Test](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStream_close_0_0_Test.java) | échec compilation | 4 | `when(void)` → `doThrow().when()`, `assertThrows`, 2 oracles faux |
| [CharsetUtils_clean_2_0_Test](tika-core/src/test/java/org/apache/tika/utils/CharsetUtils_clean_2_0_Test.java) | échec exécution | 5 | 12 `when(method.invoke(...))` impossibles supprimés, réflexion sur un champ inexistant supprimée, 3 oracles faux |

Total : 7 classes de tests générés (39 méthodes de test, dont 1 désactivée) et 16 corrections manuelles.

## 6. Critique des oracles générés vs. écrits à la main

### 6.1 Les tests écrits à la main (originaux)

Les tests de `LookaheadInputStreamTest` utilisent de vrais flux (`ByteArrayInputStream`) et vérifient l'**état observable** : chaque octet lu, `-1` en fin de lookahead, et surtout que le flux sous-jacent est rembobiné après `close()` (`assertEquals('a', stream.read())`, etc.). Ces oracles sont spécifiques et reflètent le contrat réel de la classe : lire en avance sans consommer. `CharsetUtilsTest` utilise des tables de cas concrets (`"win-1251"` → `"windows-1251"`, `"koi8r"` → `"KOI8-R"`) qui encodent la connaissance du domaine.

### 6.2 Les tests générés : défauts observés

1. **Oracles d'interaction plutôt que d'état.** Presque tous les tests de LookaheadInputStream mockent `InputStream` et vérifient les appels (`verify(mock).read(...)`) au lieu des octets produits. Ces oracles sont fragiles, car ils dépendent de l'implémentation, et souvent faux : le modèle suppose que `read(b, off, len)` est délégué tel quel au flux, alors que la classe lit dans son tampon interne de taille `n` (erreur systématique de `read_3_0`, `read_3_1`, `skip_4_1`).
2. **Comportements inventés.** Le modèle attend une `NullPointerException` pour un flux `null` (la classe le traite comme un flux vide), un appel à `stream.close()` dans `close()` (la classe rembobine sans fermer), et des appels à `stream.available()` qui n'existent pas.
3. **Vérifications faibles ou triviales.**
   - `markSupported_6_0` se résume à `assertTrue(markSupported())`. C'est correct mais trivial, et il tue 1 mutant.
   - `read_3_0` vérifie le **nombre** d'octets renvoyés, jamais **leur contenu**. Le mock ne remplit d'ailleurs jamais le tampon. Résultat : le mutant qui supprime `System.arraycopy` (L106) survit.
4. **Tests trompeurs.** Dans `CharsetUtils_isSupported_1_1_Test`, le modèle crée un mock local de `Method isSupportedICU`, mais ne l'injecte jamais dans `CharsetUtils`. Les tests `testIsSupportedWithICUSupportedCharset`/`...Unsupported` passent, mais testent en réalité le chemin JDK : leur nom ment sur ce qu'ils vérifient. De même, `clean_2_0` tente de stubber un vrai objet `Method` avec `when(...)`, ce qui est impossible.
5. **Redondance.** Les valeurs testées pour CharsetUtils (`"UTF-8"`, `null`, `""`, `"none"`, `"no"`, `"win-1252"`) reprennent presque toutes celles de `CharsetUtilsTest`. Les tests générés pour CharsetUtils n'ont tué **aucun** nouveau mutant.
6. **Méconnaissance de l'API Java.** Noms canoniques erronés : `clean("cp-1252")` attendu `"cp1252"` au lieu de `"windows-1252"`, `clean("utf-8")` attendu `"utf-8"` au lieu de `"UTF-8"`.

### 6.3 Ce que les tests générés apportent quand même

- Ils exercent les méthodes **jamais couvertes** (`read(byte[],int,int)`, `markSupported`) et tuent 6 mutants `NO_COVERAGE` (section 8).
- Ils testent la **propagation des exceptions** (`IOException` levée par le flux sous-jacent pendant `read` et `close`), un cas absent des tests originaux.
- Deux oracles « faux » du LLM révèlent en fait des **écarts de comportement réels** dans Tika :
  - `testSkipWithNegativeInput` attend `skip(-3) == 0`, ce qui est conforme au contrat de `InputStream.skip`. Or `LookaheadInputStream.skip(-3)` renvoie `-3` et **recule** la position. J'ai désactivé ce test (`@Disabled`) en documentant l'écart, plutôt que d'aligner l'oracle sur un comportement douteux.
  - `testCleanWithISO8859Pattern` suppose que `clean("ISO 8859-1")` fonctionne, comme le promet le commentaire du code (« Handle "iso 8859-x" error »). En réalité, `CHARSET_NAME_PATTERN` coupe le nom au premier espace (`"ISO"`), donc `clean` renvoie `null` : la branche annoncée ne gère pas ce cas.

En résumé : les oracles générés sont surtout génériques, dépendants de l'implémentation et souvent faux. Ils sont utiles pour la couverture, mais pas pour la détection fine de fautes. Les oracles écrits à la main sont plus spécifiques et plus robustes.

## 7. pitest : configuration

Ajouté dans [tika-core/pom.xml](tika-core/pom.xml) : `pitest-maven:1.30.0` avec `pitest-junit5-plugin:1.2.3`, mutateurs `DEFAULTS`, rapports HTML et XML. Classes et tests ciblés, définis par propriétés et surchargeables avec `-DtargetClasses=... -DtargetTests=...` :

```xml
<targetClasses>org.apache.tika.io.LookaheadInputStream,org.apache.tika.utils.CharsetUtils</targetClasses>
<targetTests>org.apache.tika.io.LookaheadInputStream*,org.apache.tika.utils.CharsetUtils*</targetTests>
```

Le filtre de tests limite l'analyse aux tests propres à chaque classe (originaux `*Test`, générés `*_N_M_Test`, manuels `*ManualTest`). C'est ainsi que les trois mesures du tableau 1 sont obtenues : en ajoutant progressivement les fichiers de test.

## 8. Mutation : tests originaux vs. tests générés

| Classe | Originaux | + générés | Δ |
|---|---|---|---|
| LookaheadInputStream | 19/33 (58 %) | 25/33 (76 %) | +6 |
| CharsetUtils | 15/25 (60 %) | 15/25 (60 %) | 0 |

**Les tests générés ne détectent pas tous les mutants** : il en reste 8 vivants dans LookaheadInputStream et 10 dans CharsetUtils.

### Mutants tués par les tests générés, et pourquoi

| # | Ligne | Mutant | Tué par | Raison |
|---|---|---|---|---|
| 1 | L129 | `markSupported` renvoie `false` | `markSupported_6_0.testMarkSupported` | assertion directe `assertTrue(markSupported())` |
| 2 | L103 | appel `fill()` supprimé dans `read(byte[],..)` | `read_3_0.testReadWithPartialBufferedData` | sans `fill()`, le tampon reste vide et `read` renvoie `-1`, alors que l'oracle attend `3` (le mock fournit 3 octets) |
| 3 | L104 | `buffered > position` remplacé par `false` | `read_3_0.testReadWithPartialBufferedData` | la méthode renverrait toujours `-1` au lieu de `3` |
| 4 | L104 | borne `>` → `>=` | `read_3_0.testReadWithNoBufferedData` | le mock renvoie `-1`, donc `buffered = position = 0`. Avec `>=`, on entre dans la branche et la méthode renvoie `0` au lieu de `-1` |
| 5 | L108 | `return len` → `return 0` | `read_3_0.testReadWithPartialBufferedData` | l'oracle attend le nombre d'octets lus (`3`) |
| 6 | L110 | `return -1` → `return 0` | `read_3_0.testReadWithNoBufferedData` | l'oracle de fin de flux attend `-1` |

Les 6 mutants tués étaient tous `NO_COVERAGE` : les tests générés gagnent en **couverture**, pas en finesse. Ils ne tuent aucun mutant qui survivait déjà dans du code couvert.

### Mutants qui survivent aux tests générés, et pourquoi

- **L106** (`System.arraycopy` supprimé) et **L105** (`buffered - position` → `+`) : le mock ne fournit jamais de vrais octets et aucun test n'examine le contenu de `b`. Un seul `read` est fait par test, donc `position = 0` et `buffered + position == buffered - position`.
- **L107** (`position += len` → `-=`) : aucun test ne fait deux lectures successives.
- **L67, L68, L81, L82, L86** : déjà vivants avant, voir section 9. Les mocks renvoient toujours le même nombre d'octets et ne modélisent pas `mark`/`reset`.
- **CharsetUtils** : les tests générés reprennent les entrées des tests originaux et n'atteignent pas les branches ICU (mock jamais injecté, voir 6.2).

## 9. Tests ajoutés à la main

Deux classes : [LookaheadInputStreamManualTest](tika-core/src/test/java/org/apache/tika/io/LookaheadInputStreamManualTest.java) (6 tests) et [CharsetUtilsManualTest](tika-core/src/test/java/org/apache/tika/utils/CharsetUtilsManualTest.java) (11 tests). Elles tuent **les 16 mutants non équivalents restants**.

### 9.1 LookaheadInputStreamManualTest

Deux flux de test servent de doublures « réalistes » (pas de mock) :
- `OneByteAtATimeInputStream` renvoie au plus 1 octet par appel, comme un flux réseau lent ;
- `NoZeroLengthReadInputStream` échoue si on lui demande de lire 0 octet.

**`testMarkIsSetAtCurrentPositionOfUnderlyingStream`**
- *Intention* : le constructeur doit poser une marque sur le flux sous-jacent **à sa position courante**, pour que `close()` y revienne.
- *Données* : flux `"abc"` dont on a déjà consommé `'a'` avant de créer le lookahead (n=1). Si le flux n'est pas lu au préalable, la marque implicite de `ByteArrayInputStream` (position 0) masque l'absence d'appel à `mark`.
- *Oracle* : après lecture de `'b'` puis `close()`, le flux doit relire `'b'`. Sans `mark(n)`, `reset()` ramène à 0 et on lirait `'a'`.
- *Mutants tués* : L67 (`stream != null` → `false`), L68 (appel `mark` supprimé).

**`testFillsBufferFromStreamReturningOneByteAtATime`**
- *Intention* : `fill()` doit compléter le tampon par petits morceaux quand le flux renvoie moins d'octets que demandé.
- *Données* : `OneByteAtATimeInputStream("abcd")` et n=3. Il faut 3 appels à `fill`, et à partir du 2e, `buffered > 0`.
- *Oracle* : on lit `'a'`, `'b'`, `'c'` puis `-1` (lookahead de 3), et après `close()` le flux sous-jacent est intact (`'a'`). Le flux de test vérifie les bornes comme l'exige le contrat d'`InputStream.read`. Avec `buffer.length + buffered` (mutant L82), la longueur demandée au 2e appel (`3 + 1`) dépasse le tableau, ce qui lève `IndexOutOfBoundsException`.
- *Mutant tué* : L82 (`-` → `+`). Il est aussi tué par le test suivant.

**`testNoReadFromStreamOnceBufferIsFull`**
- *Intention* : une fois le tampon plein, la classe ne doit plus interroger le flux sous-jacent.
- *Données* : `NoZeroLengthReadInputStream("abc")` avec n=2. Après 2 lectures, `buffered == buffer.length`.
- *Oracle* : `read()` renvoie `-1` deux fois, sans exception. Avec la borne `<=` (mutant L81), `fill()` appelle `read(buffer, 2, 0)` et le flux lève une `AssertionError`.
- *Mutant tué* : L81 (`<` → `<=`).

**`testUnderlyingStreamIsResetAsSoonAsEndOfStreamIsReached`**
- *Intention* : quand le flux sous-jacent est épuisé avant la fin du lookahead, `fill()` appelle `close()` immédiatement et le flux est rembobiné **sans attendre** que l'utilisateur ferme le lookahead.
- *Données* : flux `"ab"` plus court que le lookahead (n=10), pour forcer la branche `n == -1`.
- *Oracle* : après avoir lu `'a'`, `'b'`, `-1`, **sans** appeler `lookahead.close()`, `stream.read()` doit renvoyer `'a'`. Sans l'appel à `close()` (mutant L86), le flux reste en fin et renvoie `-1`.
- *Mutants tués* : L86 (appel `close` supprimé), L82.

**`testReadArrayCopiesAtMostBufferedBytesAtOffset`**
- *Intention* : `read(b, off, len)` copie les octets **au bon décalage**, au plus `buffered - position`, et avance la position.
- *Données* : flux `"abcde"`, n=3, tableau pré-rempli de `'x'` pour voir exactement ce qui est écrit. Deux lectures successives :
  - `(b, 1, 2)` : moins que le disponible, avec un décalage non nul ;
  - `(b, 0, 10)` : plus que le disponible, il ne reste qu'un octet.
- *Oracle* :
  - la 1re lecture renvoie `2` et `b == {x, a, b, x, x, x}` ;
  - la 2e renvoie `1` et `b[0] == 'c'` ;
  - la 3e renvoie `-1`.
  - Correspondance avec les mutants : sans `arraycopy` (L106), `b` reste plein de `'x'`. Avec `buffered + position` (L105), on copie 5 octets depuis un tampon de 3, ce qui lève une exception. Avec `position -= len` (L107), la 2e lecture renvoie de mauvais octets.
- *Mutants tués* : L105, L106, L107.

**`testMarkSupported`**
- *Intention* : contrat explicite de la classe (`mark`/`reset` sont supportés), avec un flux `null`.
- *Oracle* : `true`. Il fait double emploi avec le test généré, mais rend la suite manuelle autonome.

### 9.2 CharsetUtilsManualTest

**`testNoneIsRejectedAsIllegalName`, `testNoIsRejectedAsIllegalName`**
- *Intention* : `forName` rejette explicitement les valeurs `"none"`/`"no"`, fréquentes dans les en-têtes HTML (`charset=none`), avec une `IllegalCharsetNameException`.
- *Données* : `"none"`, `"NONE"` (vérifie la mise en minuscules) et `"no"`.
- *Oracle* : le **type précis** de l'exception. Sans la branche L159, on arrive à `Charset.forName("none")`, qui lève `UnsupportedCharsetException`, une sœur de `IllegalCharsetNameException` (les deux héritent d'`IllegalArgumentException`). C'est pourquoi `clean()`, qui renvoie `null` dans les deux cas, ne pouvait pas tuer ce mutant.
- *Mutant tué* : L159.

**`testUnknownNameIsUnsupportedNotIllegal`, `testNullNameIsIllegalArgument`, `testNameNotMatchingPatternIsIllegal`, `testForNameReturnsCharsetObject`**
- *Intention* : fixer le contrat de `forName` sur ses autres chemins, à savoir :
  - nom inconnu → `UnsupportedCharsetException` ;
  - `null` → `IllegalArgumentException` ;
  - chaîne d'espaces qui ne correspond pas au motif → `IllegalCharsetNameException` ;
  - nettoyage des guillemets et des `;` ;
  - nom `"8859-1"` → `ISO_8859_1` via `ISO_NAME_PATTERN`.
- *Oracle* : objets `Charset` de `StandardCharsets` et types d'exception définis par l'API `java.nio.charset`. Ces tests servent à éviter les régressions et à documenter le contrat ; ils ne tuent pas de nouveau mutant.

**Tests ICU : `testIcuIsSupportedTakesPrecedenceOverJdk`, `testIcuNotSupportedFallsBackToJdk`, `testIcuFailureMakesCharsetUnsupported`, `testIcuForNameUsedWhenNotACommonCharset`, `testIcuReturningNullFallsBackToJdk`**
- *Intention* : tester la logique de délégation à ICU4J sans ICU4J. CharsetUtils mémorise dans deux champs statiques privés (`isSupportedICU`, `getCharsetICU`) les `Method` trouvées par réflexion. Le test y installe, aussi par réflexion, les méthodes d'une classe `FakeIcu` aux mêmes signatures que `CharsetICU`. Les champs sont sauvegardés en `@BeforeEach` et restaurés en `@AfterEach` pour ne pas contaminer les autres tests.
- *Données* :
  - `"x-fake-icu"` : nom que seul le faux ICU connaît, inconnu du JDK, ce qui distingue les deux chemins ;
  - `"boom"` : le faux ICU lève une exception, ce qui teste le `catch (Exception)` ;
  - `"UTF-8"`/`"bogus"`/`"ascii"` : le faux ICU répond non ou `null`, ce qui teste le repli sur le JDK.
- *Oracle* :
  - `isSupported("x-fake-icu")` passe de `false` à `true` une fois ICU installé ;
  - `isSupported("boom")` renvoie `false`, car la méthode ne doit jamais lever d'exception ;
  - `forName("x-fake-icu") == UTF_8`, c'est-à-dire la valeur renvoyée par ICU ;
  - si ICU renvoie `null`, on obtient le résultat du JDK ou `UnsupportedCharsetException`.
- *Mutants tués* : L107 (`isSupportedICU != null` → `false`), L108 (`return true` → `false`), L118 (`return false` → `true` dans le `catch`), L183 (`getCharsetICU != null` → `false`), L186 (`cs != null` → `false`), L187 (`return cs` → `null`).

## 10. Mutants restants (équivalents)

Deux mutants de `CharsetUtils.forName` survivent. Ils sont **équivalents** : aucune entrée ne peut les distinguer du code original.

- **L157** `if (charset != null) return charset;` (après la recherche dans `COMMON_CHARSETS`) remplacé par `false`. Si le nom est dans le cache, `charset` est non nul. Le mutant passe aux branches `iso`/`cp`/`win`, qui ne réaffectent `charset` qu'avec `COMMON_CHARSETS.get(...)` d'un alias du **même** charset. La vérification suivante L178 renvoie donc le même objet. Le seul effet est un léger surcoût.
- **L178** `if (charset != null) return charset;` (après normalisation `iso`/`cp`/`win`) remplacé par `false`. Le code continue vers ICU (absent) puis `Charset.forName(name)` avec le nom normalisé, qui renvoie le même objet `Charset`, puisque `COMMON_CHARSETS` est construit à partir de `Charset.forName`. Avec un faux ICU installé, ce dernier renvoie `null` pour ces noms et on retombe sur le même chemin.

## 11. GitHub Action

Workflow : [.github/workflows/tache2.yml](.github/workflows/tache2.yml). Il se déclenche à chaque push sur `tache2`, sur les pull requests vers `tache2` et manuellement. Étapes :

1. JDK 21 (Temurin), avec cache Maven.
2. `./mvnw -pl tika-core -am install -DskipTests` : construction de tika-core et du pom parent.
3. `mvn test -Dtest='LookaheadInputStream*Test,CharsetUtils*Test'` : **tous les tests** des deux classes (7 classes originales, générées et manuelles pour LookaheadInputStream, 4 pour CharsetUtils ; 66 tests, 1 désactivé).
4. `mvn org.pitest:pitest-maven:mutationCoverage` : analyse de mutation finale.
5. Publication du rapport pitest comme artefact `pitest-report`.

Les workflows d'origine de Tika ne se déclenchent que sur `main` et ne sont donc pas exécutés sur cette branche.

Dernière exécution vérifiée : [run 38442437426](https://github.com/yns-bkh/tika/actions/runs/38442437426), réussie : 66 tests, 0 échec, 1 désactivé ; pitest 56/58 mutants tués (97 %). Toutes les exécutions : [onglet Actions](https://github.com/yns-bkh/tika/actions/workflows/tache2.yml).

## 12. Déclaration d'utilisation de l'IA

Conformément aux consignes de l'Université de Montréal (« Intelligence artificielle générative : citer, signaler, déclarer et documenter ») :

- **ChatUniTest 2.1.1 + qwen2.5-coder:7b (Alibaba, via Ollama, exécuté localement)** : outil imposé par l'énoncé, utilisé pour générer les tests `*_N_M_Test.java`. Ses sorties brutes sont archivées dans `tache2-rapports/chatunitest-brut/` et chaque modification manuelle est marquée `// CORRECTION`.
- **Claude Code (Anthropic, modèle Claude Opus 5.5), 13 octobre 2026** : assistant utilisé pour installer Ollama et configurer ChatUniTest et pitest dans Maven, diagnostiquer les erreurs des tests générés, proposer les corrections minimales et les tests manuels, écrire le workflow GitHub Actions et rédiger ce README. Tous les résultats (exécution des tests, scores de mutation) proviennent d'exécutions réelles, dont les rapports sont inclus dans le dépôt. J'ai relu et validé l'ensemble.
