# Subdomain Content-Scan (Reputationsschutz)

**Problem:** Free-User können beliebige Subdomains registrieren → Reputationsrisiko, wenn dahinter problematische Inhalte gehostet werden.

**Idee:** Periodisch den Inhalt hinter jeder Subdomain abrufen, von der KI bewerten lassen (1-Satz-Beschreibung + Risiko-Einschätzung) und persistieren.

## Pipeline

```
Fetch (WebClient) → KI-Verdikt (Haiku 4.5, Batch) → Persistenz → Admin-Review
```

1. **Fetch:** GET `https://<fqdn>`, Text extrahieren (Title/Meta/sichtbar), `contentHash`. Unveränderter Hash → kein KI-Call.
2. **KI:** Structured Output → `{description, risk (OK/REVIEW/PROBLEMATIC), category, confidence}`. Via **Batches-API** (asynchron, ~50 % günstiger, nicht latenzkritisch).
3. **Persistenz:** `SubdomainScanEntity` (Verlauf je Scan) + `lastScanRisk`/`lastScannedAt` auf `SubdomainEntity`.
4. **Review:** Admin-Queue sortiert nach Risiko; Sperrung manuell über `SubdomainStatus`. Kein Auto-Suspend.

## Betrieb

- **Trigger:** K8s **CronJob** → interner Admin-Endpoint (bestehendes Monitoring-/GitOps-Muster, API-Key via SealedSecret).
- **Zwei Phasen:** *Submit* (Fetch + Batch einreichen) / *Ingest* (Batch pollen + Ergebnisse speichern).
- **Scope:** nur Free-Tier; batchweise übers Intervall verteilt.

## Eckpunkte

- Nur Verdikt + Hash speichern (kein voller Seiteninhalt), Scan in ToS abdecken (DSGVO).
- Metriken (Micrometer → Grafana): Anzahl je Risiko, Fehlerrate, Kosten.
