import i18n from "i18next";
import { initReactI18next } from "react-i18next";

export const defaultLanguage = "de";

void i18n.use(initReactI18next).init({
  fallbackLng: defaultLanguage,
  lng: defaultLanguage,
  interpolation: {
    escapeValue: false,
  },
  resources: {
    de: {
      translation: {
        appName: "WerkPilot VLBG",
        appSubtitle: "Entscheidungsunterstützung für Fertigungsteams",
        nav: {
          dashboard: "Dashboard",
          login: "Anmeldung",
          imports: "CSV-Import",
          masterData: "Stammdaten",
          maintenance: "Instandhaltung",
          reports: "Berichte",
          administration: "Administration",
        },
        dashboard: {
          title: "Dashboard",
          description:
            "Backend-Werte werden hier nur visualisiert; KPI-Berechnungen bleiben serverseitig.",
          producedUnits: "Produzierte Einheiten",
          energyPerUnit: "Energie pro Einheit",
          scrapRate: "Ausschussquote",
          downtime: "Stillstand",
          sampleTrend: "Beispieltrend",
          tableFallback: "Tabellarische Alternative",
        },
        imports: {
          title: "CSV-Import",
          description:
            "CSV-Dateien hochladen, Importstatus verfolgen und Fehler einsehen. Korrektur und Rollback sind ADMIN vorbehalten.",
          type: "Importtyp",
          file: "CSV-Datei",
          submit: "Import starten",
          required: "Bitte wählen Sie eine CSV-Datei aus.",
          uploadStarted: "Der Import wurde gestartet und wird verarbeitet.",
          readOnly:
            "Lesemodus: Ihre Rolle erlaubt keine CSV-Uploads. Korrektur und Rollback sind ADMIN vorbehalten.",
          production: "Produktion",
          energy: "Energie",
          downtime: "Stillstand",
          scrap: "Ausschuss",
          typeLabels: {
            PRODUCTION_RECORDS: "Produktion",
            ENERGY_MEASUREMENTS: "Energie",
            DOWNTIME_RECORDS: "Stillstand",
            SCRAP_RECORDS: "Ausschuss",
          },
          status: {
            PROCESSING: "In Verarbeitung",
            COMMITTED: "Festgeschrieben",
            FAILED: "Fehlgeschlagen",
            SUPERSEDED: "Ersetzt",
          },
          history: {
            title: "Importverlauf",
            description:
              "Alle Import-Jobs mit Status, Zeilenzahlen und Fehlern. Laufende Jobs werden automatisch aktualisiert.",
            type: "Typ",
            file: "Datei",
            status: "Status",
            rows: "Zeilen (gültig/gesamt)",
            errors: "Fehler",
            createdAt: "Gestartet am",
            actions: "Aktionen",
            empty: "Noch keine Importe vorhanden.",
            previous: "Zurück",
            next: "Weiter",
          },
          actions: {
            showErrors: "Fehler anzeigen",
            correct: "Korrigieren",
            rollback: "Zurückrollen",
          },
          errorsPanel: {
            title: "Fehlerdetails",
            loading: "Fehler werden geladen...",
            overflow:
              "Es werden maximal 500 Fehler gespeichert; weitere Fehler wurden gezählt, aber nicht aufgezeichnet.",
            row: "Zeile",
            column: "Spalte",
            value: "Wert",
            message: "Meldung",
          },
          correctionPanel: {
            title: "Import korrigieren",
            description:
              "Eine gültige Ersatzdatei ersetzt den Import vollständig und atomar. Der ursprüngliche Job wird als Ersetzt markiert.",
            submit: "Korrektur hochladen",
          },
          rollbackPanel: {
            title: "Import zurückrollen",
            description:
              "Der Import wird ohne Ersatz als Ersetzt markiert und zählt nicht mehr für KPIs. Eine Begründung ist erforderlich.",
            reason: "Begründung",
            reasonRequired: "Bitte geben Sie eine Begründung an (max. 500 Zeichen).",
            submit: "Rollback ausführen",
          },
        },
        placeholders: {
          masterData:
            "Stammdatenmasken werden nach Identity und RBAC umgesetzt.",
          maintenance:
            "Tickets, Zuständigkeiten und Fälligkeiten folgen in Sprint 4.",
          reports: "Monatsberichte und Downloads folgen in Sprint 5.",
          administration:
            "Benutzer, Rollen und Einstellungen folgen in Sprint 1.",
        },
        shell: {
          routeReady: "Bereich vorbereitet",
          apiStatus: "API-Client Basis: /api",
        },
        notFound: {
          title: "Seite nicht gefunden",
          description:
            "Die angeforderte Seite ist in WerkPilot VLBG nicht vorbereitet.",
          action: "Zurück zum Dashboard",
        },
      },
    },
  },
});

export { i18n };
