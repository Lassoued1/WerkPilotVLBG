import { Link } from "react-router";
import { useTranslation } from "react-i18next";

export function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <section className="page-stack" aria-labelledby="not-found-title">
      <div className="page-heading">
        <h1 id="not-found-title">{t("notFound.title")}</h1>
        <p>{t("notFound.description")}</p>
      </div>

      <Link className="primary-button primary-link" to="/">
        {t("notFound.action")}
      </Link>
    </section>
  );
}
