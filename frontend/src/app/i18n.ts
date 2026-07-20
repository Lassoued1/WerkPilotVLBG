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
          machines: "Maschinen",
          production: "Produktion",
          imports: "CSV-Import",
          masterData: "Stammdaten",
          anomalies: "Anomalien",
          maintenance: "Instandhaltung",
          reports: "Berichte",
          administration: "Administration",
        },
        dashboard: {
          title: "Dashboard",
          description:
            "Backend-Werte werden hier nur visualisiert; KPI-Berechnungen bleiben serverseitig.",
          producedUnits: "Produzierte Einheiten",
          outputPerHour: "Ausbringung pro Stunde",
          energyPerUnit: "Energie pro Einheit",
          energyTotal: "Energie gesamt",
          scrapRate: "Ausschussquote",
          scrapTotal: "Ausschuss gesamt",
          availability: "Verfügbarkeit",
          downtime: "Stillstand",
          productionTrend: "Produktionstrend",
          downtimePareto: "Stillstands-Pareto",
          energyTopConsumers: "Top-Energieverbraucher",
          backendOnly: "Vom Backend berechnet",
          loading: "Dashboard-Daten werden geladen...",
          emptyTrend: "Keine Trenddaten für den gewählten Zeitraum.",
          chartFallbackHelp:
            "Das Diagramm ist rein visuell; die Tabelle darunter ist die zugängliche Alternative.",
          tableFallback: "Tabellarische Alternative",
          filters: {
            from: "Von",
            to: "Bis",
            factory: "Werk-ID",
            line: "Linien-ID",
            machine: "Maschinen-ID",
            product: "Produkt-ID",
            shift: "Schicht-ID",
            apply: "Filter anwenden",
          },
          units: {
            pieces: "Stk.",
            minutes: "Min.",
          },
          table: {
            period: "Zeitraum",
            units: "Einheiten",
            reason: "Grund",
            minutes: "Minuten",
            cumulative: "Kumuliert",
            asset: "Anlage",
            energy: "Energie",
          },
        },
        machines: {
          title: "Maschinenmonitoring",
          description:
            "Maschinenbezogene Karten und Verbraucherlisten aus dem Dashboard-Backend. Die UI berechnet keine KPI-Formeln.",
          units: "Einheiten im Filter",
          availability: "Verfügbarkeit",
          energy: "Energie",
          downtime: "Stillstand",
          topConsumers: "Energieverbrauch nach Maschine/Linie",
          empty: "Keine Maschinenwerte für den gewählten Zeitraum.",
        },
        production: {
          title: "Produktionsdatensätze",
          description:
            "Produktionsverlauf, Datensätze und Traceability bis zum Import-Job. KPI-Werte kommen vom Backend.",
          trend: "Produktionsverlauf",
          records: "Produktionsdatensätze",
          traceability:
            "Jeder Datensatz zeigt Batch und Import-Job, damit CSV-Herkunft und Korrekturen nachvollziehbar bleiben.",
          evidenceCsv: "Evidence CSV herunterladen",
          loadingRecords: "Produktionsdatensätze werden geladen...",
          empty: "Keine Produktionsdatensätze für den gewählten Zeitraum.",
          table: {
            period: "Intervall",
            units: "Einheiten",
            machine: "Maschine",
            product: "Produkt",
            batch: "Batch",
            importJob: "Import-Job",
          },
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
