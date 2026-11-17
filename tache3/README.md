# IFT3913 - Tache 3 : Semgrep dans la CI de Apache Tika

Auteur : Yanis Boukhili (equipe BOUKHILI_SOLO)

Depot : https://github.com/yns-bkh/tika, branche `tache3`

## 1. Workflow GitHub Actions

### Ce qui a ete modifie

Le fichier modifie est `.github/workflows/tache2.yml`. C'est le workflow du cas d'etude deja adapte a la tache 2. Il compile `tika-core`, lance les tests et l'analyse de mutation pitest.

Changements :

- Declenchement sur les branches `tache2` et `tache3` (push et pull request), en plus du lancement manuel.
- Permissions reduites a `contents: read`.
- Nouveau job `semgrep`, independant du job de tests.
- Nouvelle etape rickroll dans le job de tests (voir section 4).

Le job `semgrep` :

1. Recupere tout l'historique git (`fetch-depth: 0`).
2. Installe Semgrep 1.185.0 avec pip. C'est la meme version qu'en local.
3. Determine le commit de base :
   - pull request : `github.event.pull_request.base.sha` ;
   - push : `github.event.before` ;
   - premier push d'une branche (`before` vaut `0000...`) ou lancement manuel : le parent `HEAD~1`.
4. Produit deux rapports complets, a titre informatif : `semgrep-avant.json` (commit de base, dans un worktree temporaire) et `semgrep-apres.json` (commit analyse).
5. Lance `semgrep scan --baseline-commit <base> --error`. Cette etape echoue si au moins une alerte est nouvelle par rapport a la base.
6. Publie les trois rapports JSON en artefact `semgrep-rapports`, meme en cas d'echec.

Options de Semgrep, identiques en local et en CI : `--metrics=off --config p/default`, sur `tika-core/src/main/java`.

### Pourquoi ces choix

**Modifier le workflow de la tache 2 plutot qu'un workflow Apache.** Les workflows `main-*` de Tika ne se declenchent que sur `main`. Ils lancent le build complet du projet, environ 60 minutes. Le workflow de la tache 2 contient deja le job de tests du cas d'etude, ou la tache demande d'invoquer le rickroll.

**Ne bloquer que les nouvelles alertes.** Avant toute modification, Semgrep trouve deja 8 alertes dans le perimetre analyse (`tika-core/src/main/java`, regles `p/default`). Un build qui echoue des qu'il existe une alerte serait toujours rouge. Il ne dirait rien sur le commit courant.

**`--baseline-commit` plutot qu'une comparaison maison de deux rapports.** Semgrep analyse les fichiers modifies au commit courant et au commit de base. Il ne garde que les alertes absentes de la base. La correspondance ne depend pas du numero de ligne. C'est important : ajouter 13 lignes dans `XMLReaderUtils.java` a decale les 3 alertes deja presentes dans ce fichier (lignes 307, 828, 872 devenues 320, 841, 885). Semgrep ne les a pas signalees comme nouvelles.

**Job separe.** Le job `semgrep` ne depend pas de la compilation Maven. Il dure environ une minute. Un echec Semgrep et un echec de test restent distincts dans l'interface.

**Version epinglee.** Une nouvelle version de Semgrep pourrait changer les resultats sans changement de code. La version est donc fixee.

**Regles du registre (`p/default`).** C'est l'ensemble de regles par defaut. Il couvre les 7 types de vulnerabilites. Les regles sont telechargees a chaque execution et peuvent evoluer. Ce n'est pas un probleme pour le blocage : la base et le commit courant sont analyses avec les memes regles, dans le meme run.

### Limites connues

- Seul `tika-core/src/main/java` est analyse. Les autres modules et les tests ne le sont pas.
- Lors du premier push d'une branche, seul le dernier commit est compare a son parent.
- Pour un push de plusieurs commits, c'est l'ensemble du push qui est compare a `before`. Le build echoue, mais il ne dit pas quel commit a introduit l'alerte.
- Toutes les severites bloquent (INFO, WARNING, ERROR).
- Les regles sont telechargees du registre Semgrep. Sans reseau, le job echoue.

### Validation

1. Test local du mecanisme, dans un depot jetable contenant une copie de `tika-core/src/main/java` : base sans vulnerabilites, puis commit avec les 7. Semgrep trouve 12 alertes dans les fichiers modifies, en ecarte 3 qui existaient deja et rapporte 9 nouvelles alertes. Code de sortie 1. Le meme commit compare a lui-meme donne le code de sortie 0.
2. Push des commits 1 et 2 (workflow et rickroll, sans vulnerabilite). Run vert : https://github.com/yns-bkh/tika/actions/runs/44173455366 (commit de base `a1521d0`, commit analyse `ab83bbf`, 0 fichier Java modifie, 0 alerte)
3. Push du commit 3 (les 7 vulnerabilites). Le job `semgrep` est rouge et liste seulement les nouvelles alertes. Le job de tests reste vert. Run rouge : https://github.com/yns-bkh/tika/actions/runs/44174271365 (commit de base `ab83bbf`, commit analyse `dd8a04c`, 9 alertes bloquantes, code de sortie 1). Les 3 alertes deja presentes dans `XMLReaderUtils.java` ne sont pas rapportees.

## 2. Les 7 vulnerabilites

Tous les fichiers sont dans `tika-core/src/main/java/org/apache/tika/`. Chaque ajout est marque par le commentaire `IFT3913 tache 3 - vulnerabilite volontaire`. Les lignes viennent du rapport `reports/semgrep-apres.json`.

| # | Type | CWE | Fichier | Ligne | Regle Semgrep | Severite |
|---|---|---|---|---|---|---|
| 1 | Injection de commande | CWE-78 | `utils/ProcessUtils.java` | 299 | `command-injection-process-builder` | ERROR |
| 2 | Injection SQL | CWE-89 | `utils/MetadataDbUtils.java` | 36 | `jdbc-sqli`, `formatted-sql-string` | WARNING, ERROR |
| 3 | XXE | CWE-611 | `utils/XMLReaderUtils.java` | 283 | `documentbuilderfactory-disallow-doctype-decl-missing` | ERROR |
| 4 | Deserialisation non sure | CWE-502 | `io/UnsafeDeserializer.java` | 27 | `object-deserialization` | WARNING |
| 5 | Hachage faible | CWE-328 | `utils/WeakDigestUtils.java` | 26 | `use-of-md5` | WARNING |
| 6 | Validation TLS desactivee | CWE-295 | `utils/SslUtils.java` | 37, 41 | `insecure-trust-manager` | WARNING |
| 7 | Aleatoire faible | CWE-330 | `utils/TokenUtils.java` | 25 | `weak-random` | WARNING |

Les noms complets des regles commencent par `java.lang.security.audit.` (par exemple `java.lang.security.audit.crypto.use-of-md5.use-of-md5`).

### 1. Injection de commande (CWE-78)

- Fichier : `utils/ProcessUtils.java`, methode `runShell(String arg)`. Ce fichier gere deja les processus externes de Tika.
- Code : `new ProcessBuilder("sh", "-c", arg).start()`.
- Pourquoi c'est vulnerable : `sh -c` interprete toute la chaine comme une commande shell. Si `arg` vaut `fichier.txt; cat /etc/passwd`, le shell execute les deux commandes. L'attaquant controle ce qui s'execute sur le serveur.
- Semgrep : `command-injection-process-builder`, ligne 299.

### 2. Injection SQL (CWE-89)

- Fichier : `utils/MetadataDbUtils.java` (nouvelle classe), methode `findByTitle`. Elle cherche des documents par titre dans une base de metadonnees.
- Code : le titre est concatene dans `"SELECT id, path FROM tika_metadata WHERE title = '" + title + "'"`.
- Pourquoi c'est vulnerable : le titre vient du document analyse, donc de l'attaquant. Un titre comme `' OR '1'='1` renvoie toute la table. Un titre comme `'; DROP TABLE tika_metadata; --` peut detruire des donnees. Il faudrait un `PreparedStatement` avec parametre.
- Semgrep : `jdbc-sqli` et `formatted-sql-string`, ligne 36.

### 3. XXE (CWE-611)

- Fichier : `utils/XMLReaderUtils.java`, nouvelle methode `buildDOMLenient`. Elle est placee a cote de `getDocumentBuilderFactory()`, qui configure le parseur de facon securisee.
- Code : `DocumentBuilderFactory.newInstance()` sans desactiver les DTD ni les entites externes.
- Pourquoi c'est vulnerable : un document XML peut declarer une entite externe, par exemple `<!ENTITY x SYSTEM "file:///etc/passwd">`. Le parseur lit alors ce fichier et l'insere dans le document. Cela permet de lire des fichiers du serveur, de faire des requetes internes (SSRF) ou de saturer la memoire.
- Semgrep : `documentbuilderfactory-disallow-doctype-decl-missing`, ligne 283.

### 4. Deserialisation non sure (CWE-502)

- Fichier : `io/UnsafeDeserializer.java` (nouvelle classe), methode `read(InputStream in)`.
- Code : `new ObjectInputStream(in).readObject()`.
- Pourquoi c'est vulnerable : `readObject()` reconstruit n'importe quelle classe presente dans le classpath et execute du code pendant cette reconstruction. Un flux construit avec une chaine de "gadgets" (classes de bibliotheques courantes) peut executer du code arbitraire. Il n'y a aucune liste de classes autorisees.
- Semgrep : `object-deserialization`, ligne 27.

### 5. Hachage faible (CWE-328)

- Fichier : `utils/WeakDigestUtils.java` (nouvelle classe), methode `md5`.
- Code : `MessageDigest.getInstance("MD5")`.
- Pourquoi c'est vulnerable : on sait fabriquer deux contenus differents avec le meme hash MD5 (collisions). MD5 ne garantit donc plus l'integrite d'un fichier. Il est aussi tres rapide, ce qui facilite la force brute sur des mots de passe. Il faudrait SHA-256 ou mieux.
- Semgrep : `use-of-md5`, ligne 26.

### 6. Validation TLS desactivee (CWE-295)

- Fichier : `utils/SslUtils.java` (nouvelle classe). Elle sert a telecharger des documents depuis des serveurs a certificat auto-signe.
- Code : un `X509TrustManager` dont `checkClientTrusted` et `checkServerTrusted` sont vides, installe par defaut avec `HttpsURLConnection.setDefaultSSLSocketFactory`.
- Pourquoi c'est vulnerable : aucun certificat n'est verifie. Un attaquant sur le reseau (wifi public, proxy) presente son propre certificat et lit ou modifie tout le trafic HTTPS. Comme la fabrique est installee par defaut, toute l'application est touchee.
- Semgrep : `insecure-trust-manager`, lignes 37 et 41 (une alerte par methode vide).

### 7. Aleatoire faible (CWE-330)

- Fichier : `utils/TokenUtils.java` (nouvelle classe), methode `newToken`.
- Code : `Long.toHexString(new Random().nextLong())`.
- Pourquoi c'est vulnerable : `java.util.Random` est un generateur deterministe (congruentiel lineaire, etat de 48 bits). En observant quelques sorties, on retrouve l'etat interne et on predit les jetons suivants. Pour un jeton de session, il faut `SecureRandom`.
- Semgrep : `weak-random`, ligne 25.

## 3. Comparaison avant et apres

Perimetre analyse : le code de production de `tika-core`, soit `tika-core/src/main/java`. Les autres modules, les tests et les fichiers non Java ne sont pas analyses. C'est le meme perimetre que dans la CI.

Rapports complets : `reports/semgrep-avant.json` (commit `918d7a0`, sans vulnerabilites, 8 alertes) et `reports/semgrep-apres.json` (avec les 7 vulnerabilites, 17 alertes). Commande : `semgrep scan --metrics=off --config p/default tika-core/src/main/java`, Semgrep 1.185.0. Aucune erreur d'analyse dans les deux rapports.

| Regle | Avant | Apres |
|---|---|---|
| `command-injection-process-builder` | 2 | 3 |
| `unsafe-reflection` | 3 | 3 |
| `saxparserfactory-disallow-doctype-decl-missing` | 1 | 1 |
| `xmlinputfactory-possible-xxe` | 1 | 1 |
| `unencrypted-socket` | 1 | 1 |
| `insecure-trust-manager` | 0 | 2 |
| `formatted-sql-string` | 0 | 1 |
| `jdbc-sqli` | 0 | 1 |
| `documentbuilderfactory-disallow-doctype-decl-missing` | 0 | 1 |
| `object-deserialization` | 0 | 1 |
| `use-of-md5` | 0 | 1 |
| `weak-random` | 0 | 1 |
| **Total** | **8** | **17** |

Les 9 alertes nouvelles sont celles du tableau de la section 2. Aucune alerte n'a disparu.

Les 8 alertes deja presentes, inchangees :

| Fichier | Ligne avant | Regle |
|---|---|---|
| `config/ServiceLoader.java` | 196 | `unsafe-reflection` |
| `detect/FileCommandDetector.java` | 110 | `command-injection-process-builder` |
| `mime/MimeTypesReader.java` | 223 | `saxparserfactory-disallow-doctype-decl-missing` |
| `parser/NetworkParser.java` | 68 | `unencrypted-socket` |
| `parser/external/ExternalParser.java` | 193 | `command-injection-process-builder` |
| `utils/XMLReaderUtils.java` | 307 (320 apres) | `xmlinputfactory-possible-xxe` |
| `utils/XMLReaderUtils.java` | 828 (841 apres) | `unsafe-reflection` |
| `utils/XMLReaderUtils.java` | 872 (885 apres) | `unsafe-reflection` |

## 4. Rickroll

Action composite ecrite en bash : `.github/actions/rickroll/action.yml`. Elle n'utilise aucune action tierce et aucun acces reseau. Elle :

- ajoute une annotation d'erreur "Test en echec" dans les logs, avec le lien https://youtu.be/dQw4w9WgXcQ ;
- ecrit un message et le lien dans le resume du job (`$GITHUB_STEP_SUMMARY`).

Elle est invoquee dans le job `tests-et-mutation`, juste apres l'etape de tests, avec `if: failure()`. Elle ne s'execute donc que si la compilation ou un test echoue.

Validation, sur la branche jetable `tache3-rickroll-demo` (https://github.com/yns-bkh/tika/tree/tache3-rickroll-demo) :

- Un seul commit (`56286c0`) part de `tache3`. Il ajoute un test volontairement faux, `CharsetUtilsRickrollDemoTest` : `assertEquals("UTF-8", CharsetUtils.clean("latin1"))`. Il ajoute aussi la branche aux declencheurs du workflow.
- Run : https://github.com/yns-bkh/tika/actions/runs/44175558580
- Le job `tests-et-mutation` est rouge. Surefire rapporte `Tests run: 67, Failures: 1` et `expected: <UTF-8> but was: <ISO-8859-1>`.
- L'etape "Rickroll si un test echoue" s'execute. L'annotation d'erreur "Test en echec" avec le lien apparait sur le run, et le message est ecrit dans le resume du job.
- L'etape pitest est sautee. Le job `semgrep` reste vert, car aucun fichier de `tika-core/src/main/java` n'est modifie.

Sur `tache3`, les tests passent : l'etape rickroll est sautee (runs du push 1 et du push 2).

Cette branche n'est pas fusionnee dans `tache3`.

## Déclaration d'utilisation de l'IA

J'ai réalisé cette tâche seul. J'ai utilisé Claude Code de la façon suivante :

- Claude Code a écrit la modification du workflow `tache2.yml` (job Semgrep), l'action `rickroll`, trois des sept vulnérabilités (injection SQL, XXE, validation TLS désactivée).
- Pour les quatre autres vulnérabilités (injection de commande, désérialisation, MD5, aléatoire faible), j'ai écrit le code moi-même, ainsi que le contenu du README.
- J'ai relu et vérifié l'ensemble des changements, et j'ai lancé moi-même les exécutions de la CI.
