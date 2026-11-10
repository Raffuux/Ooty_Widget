# Programme Ooty

App Android avec un widget d'écran d'accueil qui affiche la séance du jour du plan d'entraînement
(`Planning_Ooty_2027.xlsx`), et une vue complète du plan.

- **Installer** : sur le téléphone, télécharge `ooty-widget.apk` depuis la release
  [latest](../../releases/latest), ouvre-le et autorise l'installation. Ensuite, appui long sur
  l'écran d'accueil → Widgets → Programme Ooty.
- **Mettre à jour le plan** : `python3 tools/build_assets.py chemin/vers/Planning_Ooty_2027.xlsx`, puis commit et push.
  GitHub Actions recompile l'APK à chaque push sur `main`.
