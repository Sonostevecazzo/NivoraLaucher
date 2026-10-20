# Nivora release signing — procedura sicura

La CI può generare un Android App Bundle firmato solo dopo aver configurato una chiave di release persistente. Non generare una nuova chiave per ogni build: gli aggiornamenti futuri devono mantenere la stessa identità di firma.

## 1. Genera la chiave sul tuo Mac

Installa un JDK 17 o superiore, poi esegui nel Terminale:

```bash
keytool -genkeypair -v \
  -keystore nivora-release.jks \
  -alias nivora \
  -keyalg RSA -keysize 4096 -validity 10000
```

Scegli una password robusta per il keystore e una per la chiave. Conserva il file `nivora-release.jks` e le password in un luogo sicuro, cifrato e con backup. **Non caricare il keystore nel repository, non allegarlo a issue e non inviarlo in chat.**

## 2. Prepara il secret Base64

Su macOS, dalla cartella che contiene il file:

```bash
base64 < nivora-release.jks | tr -d '\n' | pbcopy
```

Il contenuto negli appunti è sensibile. Incollalo direttamente nel secret GitHub `NIVORA_KEYSTORE_BASE64`, poi svuota gli appunti.

## 3. Configura GitHub Actions

Nel repository apri **Settings → Secrets and variables → Actions → New repository secret** e crea:

| Secret | Valore |
|---|---|
| `NIVORA_KEYSTORE_BASE64` | Contenuto Base64 del keystore |
| `NIVORA_STORE_PASSWORD` | Password del keystore |
| `NIVORA_KEY_ALIAS` | `nivora` |
| `NIVORA_KEY_PASSWORD` | Password della chiave |

Non inserire questi valori nel codice o nei file Gradle. Non condividere i valori in una conversazione.

## 4. Genera e verifica il pacchetto

Dopo aver salvato i secrets, rilancia il workflow **Nivora Android CI** da Actions con **Run workflow**. Con tutti i secrets presenti, la pipeline esegue i test e lint, crea l'AAB firmato e l'APK di release, verifica l'allineamento per dispositivi con pagine di memoria da 16 KB e pubblica l'artifact `nivora-release-aab`.

Scarica e conserva una copia dell'AAB e della chiave. Prima di caricare su Play Console, verifica l'identità di firma e prova l'app su dispositivi reali.

## Limiti

Questa procedura prepara la firma e l'artifact, ma non invia automaticamente l'app a Google Play. La pubblicazione richiede accesso alla Play Console, contatti reali dello sviluppatore, privacy policy pubblica e dichiarazioni accurate della scheda.
