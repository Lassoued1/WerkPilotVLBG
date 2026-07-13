import { useTranslation } from "react-i18next";

type PlaceholderPageProps = {
  titleKey: string;
  bodyKey: string;
};

export function PlaceholderPage({ bodyKey, titleKey }: PlaceholderPageProps) {
  const { t } = useTranslation();

  return (
    <section className="page-stack" aria-labelledby={`${titleKey}-title`}>
      <div className="page-heading">
        <h1 id={`${titleKey}-title`}>{t(titleKey)}</h1>
        <p>{t(bodyKey)}</p>
      </div>
    </section>
  );
}
