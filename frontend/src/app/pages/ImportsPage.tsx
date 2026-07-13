import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

type ImportFormValues = {
  importType: "production" | "energy" | "downtime" | "scrap";
  csvFile: FileList;
};

export function ImportsPage() {
  const { t } = useTranslation();
  const [submittedFileName, setSubmittedFileName] = useState<string | null>(
    null,
  );
  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<ImportFormValues>({
    defaultValues: {
      importType: "production",
    },
  });

  function onSubmit(values: ImportFormValues) {
    setSubmittedFileName(values.csvFile.item(0)?.name ?? null);
  }

  return (
    <section className="page-stack" aria-labelledby="imports-title">
      <div className="page-heading">
        <h1 id="imports-title">{t("imports.title")}</h1>
        <p>{t("imports.description")}</p>
      </div>

      <form className="form-panel" noValidate onSubmit={handleSubmit(onSubmit)}>
        <label className="field">
          <span>{t("imports.type")}</span>
          <select {...register("importType")}>
            <option value="production">{t("imports.production")}</option>
            <option value="energy">{t("imports.energy")}</option>
            <option value="downtime">{t("imports.downtime")}</option>
            <option value="scrap">{t("imports.scrap")}</option>
          </select>
        </label>

        <label className="field">
          <span>{t("imports.file")}</span>
          <input
            accept=".csv,text/csv"
            type="file"
            {...register("csvFile", {
              validate: (files) => files.length > 0 || t("imports.required"),
            })}
          />
        </label>
        {errors.csvFile?.message ? (
          <p className="field-error" role="alert">
            {errors.csvFile.message}
          </p>
        ) : null}

        <button className="primary-button" type="submit">
          {t("imports.submit")}
        </button>
      </form>

      {submittedFileName ? (
        <p className="success-message" role="status">
          {t("imports.accepted")} {submittedFileName}
        </p>
      ) : null}
    </section>
  );
}
