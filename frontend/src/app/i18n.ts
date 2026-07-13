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
            "Dieses Formular legt das UI-Muster fest. Verarbeitung und Berechtigungen folgen in Sprint 2.",
          type: "Importtyp",
          file: "CSV-Datei",
          submit: "Import vorbereiten",
          required: "Bitte wählen Sie eine CSV-Datei aus.",
          accepted: "Datei wurde für die spätere Importstrecke vorgemerkt.",
          production: "Produktion",
          energy: "Energie",
          downtime: "Stillstand",
          scrap: "Ausschuss",
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
