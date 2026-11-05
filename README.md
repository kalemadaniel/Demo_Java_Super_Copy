# Demo_Java_Super_Copy

Application **Java Swing** permettant de copier automatiquement les fichiers d’un dossier source vers un dossier de destination, à intervalle configurable.

Son interface s’inspire de **Demo_Java_mysql_Mirror_Server**. Le serveur de copie fonctionne indépendamment de la fenêtre.

## ✨ Fonctionnalités

- Configuration des dossiers source et destination.
- Intervalle de copie de **1 à 86 400 secondes**.
- Copie des fichiers, sous-dossiers et dossiers vides.
- Conservation des fichiers présents uniquement dans la destination.
- Conservation des anciennes versions avant une mise à jour.
- Option pour ajouter uniquement les nouveaux fichiers.
- Comparaison du contenu pour éviter les copies inutiles.
- Copie immédiate, mise en veille et reprise.
- Journal d’activité et affichage de l’état du serveur.
- Test des accès aux dossiers.
- Option de reproduction des autorisations des fichiers.
- Installation du démarrage automatique sous Windows.

## 🖼️ Aperçu

<img width="1102" height="753" alt="Apercu Super copy" src="https://github.com/user-attachments/assets/19f9af8e-1940-48f8-93c5-c5dcf3e8c8d1" />


## 🔄 Fonctionnement

```text
Dossier source
      │
      │ Copie automatique à intervalle configurable
      ▼
Dossier destination
      │
      └── .super-copy-history/
          └── Anciennes versions conservées
```

La copie s’effectue dans un seul sens : **source → destination**.

| Situation | Comportement |
|---|---|
| Nouveau fichier dans la source | Copié vers la destination |
| Contenu identique | Aucune recopie |
| Fichier modifié | Ancienne version conservée, puis mise à jour |
| Fichier supprimé dans la source | Conservé dans la destination |
| Fichier présent uniquement dans la destination | Conservé |
| Option « Conserver les fichiers existants » activée | Aucun remplacement des fichiers existants |
| Erreur d’accès à un fichier | Erreur journalisée ; traitement des autres fichiers |

L’intervalle commence après la fin du cycle précédent. Les cycles ne se chevauchent pas.

> Les fichiers utilisateur ne sont jamais supprimés automatiquement. Les fichiers temporaires internes sont nettoyés après utilisation. L’historique n’est jamais purgé automatiquement.

## 📋 Prérequis

- **Java 8 ou supérieur** pour exécuter l’application.
- **JDK 8 ou supérieur** pour compiler les sources.
- **Windows** pour utiliser les scripts fournis.
- Autorisation de lecture sur la source.
- Autorisations de création et de modification dans la destination.

Aucune bibliothèque Java externe n’est nécessaire.

## 🚀 Démarrage rapide

1. Téléchargez et décompressez le projet dans un dossier permanent.
2. Ouvrez **Lancer.bat**.
3. Sélectionnez les dossiers source et destination.
4. Configurez l’intervalle en secondes.
5. Cliquez sur **Tester les accès**.
6. Cliquez sur **Activer le serveur**.

Vous pouvez également ouvrir l’interface avec :

```bash
java -jar Demo_Java_Super_Copy.jar
```

### Commandes de l’interface

| Bouton | Action |
|---|---|
| **Parcourir…** | Sélectionner un dossier |
| **Enregistrer** | Sauvegarder les paramètres |
| **Tester les accès** | Tester les accès avec le compte de l’interface |
| **Activer le serveur** | Enregistrer la configuration et activer les copies |
| **Copier maintenant** | Activer les copies et demander un cycle immédiat |
| **Mettre en veille** | Suspendre les copies sans fermer l’interface |

## 🖥️ Fonctionnement en arrière-plan

Fermer la fenêtre **ne ferme pas le serveur de copie**.

Pour assurer le fonctionnement après déconnexion de la session et le démarrage avec Windows, utilisez l’installateur de tâche planifiée fourni.

> L’ordinateur doit rester allumé et éveillé pour effectuer les copies.

### Démarrage automatique Windows

1. Placez le projet dans son emplacement définitif.
2. Configurez et activez les copies dans l’interface.
3. Ouvrez **Windows PowerShell en tant qu’administrateur** dans le dossier du projet.
4. Exécutez :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Installer-Demarrage-Windows.ps1
```

5. Saisissez un compte Windows ayant accès aux deux dossiers.
6. Redémarrez Windows pour garantir que le serveur utilise ce compte.

Pour des dossiers strictement locaux, vous pouvez utiliser le compte **SYSTEM** :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Installer-Demarrage-Windows.ps1 -Mode Systeme
```

La tâche créée porte le nom :

```text
Demo_Java_Super_Copy
```

Elle est gérée dans le **Planificateur de tâches Windows**. Il ne s’agit pas d’un service enregistré dans `services.msc`.

Le compte choisi doit pouvoir lire le programme et écrire dans son dossier `data`.

## 🔐 Autorisations et dossiers réseau

Le serveur utilise les droits du compte Windows qui l’exécute. Il ne contourne pas les refus d’accès.

Pour un partage réseau, privilégiez un chemin UNC :

```text
\\serveur\partage\dossier
```

Les lecteurs réseau tels que `Z:` peuvent ne pas être disponibles dans la session du serveur.

- Configurez les accès réseau du compte utilisé pour le démarrage automatique.
- Aucun mot de passe n’est enregistré dans les fichiers de l’application.
- Pour une tâche exécutée avec un compte et un mot de passe, Windows gère les identifiants.
- Le compte **SYSTEM** n’utilise pas vos identifiants réseau personnels.
- Le test d’accès de l’interface peut utiliser un compte différent de celui du serveur.

### Reproduction des autorisations

Par défaut, les nouveaux fichiers héritent des autorisations du dossier de destination.

L’option **ACL / POSIX** tente de reproduire les règles d’accès des fichiers copiés, selon le système de fichiers et les droits disponibles.

Cette option ne réplique pas :

- Les propriétaires.
- Les règles d’audit.
- Les autorisations des dossiers.
- Les permissions des partages réseau.
- Les changements d’autorisations seuls sur des fichiers dont le contenu reste identique.

## 📁 Structure du projet

```text
Demo_Java_Super_Copy/
├── src/
│   └── supercopy/
│       ├── App.java
│       ├── Config.java
│       ├── CopyEngine.java
│       └── Server.java
├── test/
│   └── supercopy/
│       ├── EngineTest.java
│       ├── ServerTest.java
│       └── UiTest.java
├── nbproject/
│   └── project.xml
├── Apercu.png
├── build.xml
├── Compiler.ps1
├── Demo_Java_Super_Copy.jar
├── Installer-Demarrage-Windows.ps1
├── Lancer.bat
├── LIRE-MOI.txt
├── Serveur.bat
├── Tester.ps1
└── VERIFICATION.txt
```

Le dossier `data` est créé lors de l’utilisation :

```text
data/
├── config.properties
├── server.lock
├── status.txt
├── startup.log
└── journal-AAAA-MM-JJ.log
```

Les anciennes versions sont conservées dans la destination :

```text
.super-copy-history/<identifiant unique>/<chemin original>
```

## 🛠️ Compilation

### Avec PowerShell

```powershell
.\Compiler.ps1 -Jdk "C:\chemin\vers\jdk"
```

### Avec Apache Ant

```bash
ant jar
```

### Avec NetBeans

1. Sélectionnez **Fichier → Ouvrir un projet**.
2. Ouvrez le dossier `Demo_Java_Super_Copy`.
3. Utilisez **Construire**, **Exécuter** ou **Tester**.

Classe principale :

```text
supercopy.App
```

### Lancer uniquement le serveur

```bash
java -cp Demo_Java_Super_Copy.jar supercopy.Server
```

Ou utilisez **Serveur.bat** pour afficher une console de diagnostic.

## 🧪 Tests

Avec PowerShell :

```powershell
.\Tester.ps1 -Jdk "C:\chemin\vers\jdk"
```

Avec Ant :

```bash
ant test
```

Les tests couvrent notamment :

- La copie initiale et les noms Unicode.
- Les dossiers vides.
- La détection des changements de contenu.
- La conservation des anciennes versions.
- L’absence de propagation des suppressions.
- Le mode sans remplacement.
- Les conflits entre fichiers et dossiers.
- Le rejet des emplacements identiques ou imbriqués.
- La sauvegarde de la configuration.
- Les cycles du serveur, sa mise en veille et sa reprise.
- La prévention de plusieurs serveurs utilisant le même dossier de configuration.
- La construction et le rendu de l’interface Swing.

Les tests utilisent des dossiers dédiés.

> Le démarrage après redémarrage de Windows, les comptes de service et les partages réseau doivent être validés dans l’environnement de déploiement. Consultez `VERIFICATION.txt` pour le détail des validations effectuées.

## ⚠️ Limites à connaître

- Les liens symboliques, jonctions et fichiers spéciaux ne sont pas copiés.
- Les dossiers source et destination doivent être distincts et non imbriqués.
- La comparaison par contenu peut être longue sur de gros volumes ou un réseau lent.
- Un fichier modifié pendant la copie peut être reporté au cycle suivant.
- L’application ne fournit pas d’instantané cohérent d’une base de données ouverte.
- L’historique et les journaux augmentent avec le temps : surveillez l’espace disponible.
- Ne lancez pas plusieurs installations différentes vers la même destination.

## 👨‍💻 Auteur

**Kalema Daniel Jonathan**

📧 kalemadaniel2014@gmail.com
