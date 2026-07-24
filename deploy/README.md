# ff-trainingskarte – Deployment

## Secrets (Infisical)

Das Secret `feuerwehr-db` wird **nicht** in diesem Repository verwaltet! Es entsteht durch den Infisical Operator (CRD `InfisicalSecret`) und spiegelt das Projekt-Verzeichnis `/feuerwehr`.

Erforderliche Keys: `DB_URL`, `DB_USER`, `DB_PASSWORD`.

Beispiel fues InfisicalSecret CRD (manuell vor Deployment erstellen):

```yaml
apiVersion: infisical.com/v1alpha1
kind: InfisicalSecret
metadata:
  name: feuerwehr-db-sync
  namespace: feuerwehr
spec:
  authMethodRef:
    name: feuerwehr-auth-method
    kind: VaultAuthMethod
  secretsPath: /feuerwehr
  targetRef:
    secretName: feuerwehr-db
