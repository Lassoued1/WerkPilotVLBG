import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import type { KpiFilters } from "./api";

type FilterPanelProps = {
  filters: KpiFilters;
  onApply: (filters: KpiFilters) => void;
};

export function FilterPanel({ filters, onApply }: FilterPanelProps) {
  const { t } = useTranslation();
  const { handleSubmit, register } = useForm<KpiFilters>({ defaultValues: filters, values: filters });

  return (
    <form className="filter-panel" onSubmit={handleSubmit(onApply)}>
      <label className="field compact-field">
        <span>{t("dashboard.filters.from")}</span>
        <input aria-label={t("dashboard.filters.from")} {...register("from", { required: true })} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.to")}</span>
        <input aria-label={t("dashboard.filters.to")} {...register("to", { required: true })} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.factory")}</span>
        <input aria-label={t("dashboard.filters.factory")} {...register("factoryId")} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.line")}</span>
        <input aria-label={t("dashboard.filters.line")} {...register("lineId")} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.machine")}</span>
        <input aria-label={t("dashboard.filters.machine")} {...register("machineId")} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.product")}</span>
        <input aria-label={t("dashboard.filters.product")} {...register("productId")} />
      </label>
      <label className="field compact-field">
        <span>{t("dashboard.filters.shift")}</span>
        <input aria-label={t("dashboard.filters.shift")} {...register("shiftId")} />
      </label>
      <button className="primary-button filter-submit" type="submit">
        {t("dashboard.filters.apply")}
      </button>
    </form>
  );
}
