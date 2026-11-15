# Utilisation de l’IA

## Probleme
J'avais deux deja une clé sur ma machine qui est similaire a celle que vous nous avez fourni , alors la nouvelle ne pouvait pa etre lu par la machine , docn j'ai eu recours a l'ia pour savoir c quoi le probleme

### Prompt
La clé que j'ai defini dans les varibales d'environnements se detecte pas par ma machine aide moi a trouver le probleme 

### Réponse 
Voici une commande pour vérifier l'existance de tes clés :

foreach ($nom in 'GEMINI_KEY', 'TAVILY_API_KEY') {
"$nom : " + (-not [string]::IsNullOrWhiteSpace(
[Environment]::GetEnvironmentVariable($nom)
))
}

comme ca j'ai trouvé l'autre clé qui avait le meme nom et qui posait probleme dans la lecture de la clé