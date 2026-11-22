# Paint Desktop

Application de dessin JavaFX destinée à Java 17 ou supérieur.

## Lancer l'application

Depuis la racine du projet :

```shell
mvn clean javafx:run
```

Le goal Maven `javafx:run` démarre l'application. Le canevas démarre sur une
page blanche de 1200 × 760 pixels.

## Fonctions disponibles

- Crayon, pinceau, gomme, ligne, rectangle, rectangle plein, ellipse et triangle.
- Texte, pot de peinture contigu et pipette.
- Couleurs principale et secondaire, épaisseur du trait et échange des couleurs.
- Aperçu des formes pendant le tracé.
- Annulation/rétablissement par action, avec raccourcis Ctrl+Z et Ctrl+Y.
- Création et redimensionnement de canevas (64 à 4096 pixels par côté).
- Ouverture des images PNG, JPEG, BMP et GIF.
- Enregistrement/export en PNG ou JPEG.

L'historique est limité automatiquement en fonction de la taille du document.
Les fichiers enregistrés sont des images aplaties ; les calques et le format
de projet éditable ne font pas partie de cette première livraison.

## Tests

```shell
mvn test
```
