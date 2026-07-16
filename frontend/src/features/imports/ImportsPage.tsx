import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { germanErrorMessage, readSession } from "../../shared/api/http";
import {
  correctJob,
  fetchErrors,
  fetchJobs,
  importTypesForRoles,
  rollbackJob,
  startImport,
  type ImportJobListItem,
  type ImportTypeKey,
  type JobStatus,
} from "./api";

type UploadFormValues = {
  importType: ImportTypeKey;
  csvFile: FileList;
};

type RollbackFormValues = {
  reason: string;
};

type CorrectionFormValues = {
  csvFile: FileList;
};

type ActiveAction = { jobId: string; kind: "errors" | "correction" | "rollback" } | null;

const jobsQueryKey = ["import-jobs"];

export function ImportsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const roles = readSession()?.profile.roles ?? [];
  const allowedTypes = importTypesForRoles(roles);
  const isAdmin = roles.includes("ADMIN");
  const [page, setPage] = useState(0);
  const [activeAction, setActiveAction] = useState<ActiveAction>(null);

  const jobsQuery = useQuery({
    queryKey: [...jobsQueryKey, page],
    queryFn: () => fetchJobs(page),
    refetchInterval: (query) =>
      query.state.data?.items.some((job) => job.status === "PROCESSING") ? 2000 : false,
  });

  const invalidateJobs = () => void queryClient.invalidateQueries({ queryKey: jobsQueryKey });

  return (
    <section className="page-stack" aria-labelledby="imports-title">
      <div className="page-heading">
        <h1 id="imports-title">{t("imports.title")}</h1>
        <p>{t("imports.description")}</p>
      </div>

      {allowedTypes.length === 0 ? (
        <div className="permission-banner" role="note">
          {t("imports.readOnly")}
        </div>
      ) : null}

      <UploadForm allowedTypes={allowedTypes} onUploaded={invalidateJobs} />

      <div className="section-heading">
        <h2>{t("imports.history.title")}</h2>
        <p>{t("imports.history.description")}</p>
      </div>

      <table className="data-table">
        <thead>
          <tr>
            <th>{t("imports.history.type")}</th>
            <th>{t("imports.history.file")}</th>
            <th>{t("imports.history.status")}</th>
            <th>{t("imports.history.rows")}</th>
            <th>{t("imports.history.errors")}</th>
            <th>{t("imports.history.createdAt")}</th>
            <th>{t("imports.history.actions")}</th>
          </tr>
        </thead>
        <tbody>
          {(jobsQuery.data?.items ?? []).map((job) => (
            <JobRow
              key={job.id}
              activeAction={activeAction}
              isAdmin={isAdmin}
              job={job}
              onActionDone={() => {
                setActiveAction(null);
                invalidateJobs();
              }}
              onToggleAction={(kind) =>
                setActiveAction((current) =>
                  current?.jobId === job.id && current.kind === kind ? null : { jobId: job.id, kind },
                )
              }
            />
          ))}
        </tbody>
      </table>
      {jobsQuery.data && jobsQuery.data.items.length === 0 ? (
        <p role="status">{t("imports.history.empty")}</p>
      ) : null}

      <div className="pagination-controls">
        <button
          className="secondary-button"
          disabled={page === 0}
          onClick={() => setPage((value) => Math.max(0, value - 1))}
          type="button"
        >
          {t("imports.history.previous")}
        </button>
        <button
          className="secondary-button"
          disabled={jobsQuery.data ? page >= jobsQuery.data.totalPages - 1 : true}
          onClick={() => setPage((value) => value + 1)}
          type="button"
        >
          {t("imports.history.next")}
        </button>
      </div>
    </section>
  );
}

function UploadForm({ allowedTypes, onUploaded }: { allowedTypes: ImportTypeKey[]; onUploaded: () => void }) {
  const { t } = useTranslation();
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset,
  } = useForm<UploadFormValues>({ defaultValues: { importType: allowedTypes[0] ?? "production" } });

  const mutation = useMutation({
    mutationFn: (values: UploadFormValues) => {
      const file = values.csvFile.item(0);
      if (!file) return Promise.reject(new Error("missing file"));
      return startImport(values.importType, file);
    },
    onSuccess: () => {
      reset();
      onUploaded();
    },
  });

  const uploadAllowed = allowedTypes.length > 0;

  return (
    <form className="form-panel" noValidate onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <label className="field">
        <span>{t("imports.type")}</span>
        <select disabled={!uploadAllowed} {...register("importType")}>
          {allowedTypes.map((type) => (
            <option key={type} value={type}>
              {t(`imports.${type}`)}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>{t("imports.file")}</span>
        <input
          accept=".csv,text/csv"
          disabled={!uploadAllowed}
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
      {mutation.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(mutation.error)}
        </p>
      ) : null}
      {mutation.isSuccess ? (
        <p className="success-message" role="status">
          {t("imports.uploadStarted")}
        </p>
      ) : null}

      <button className="primary-button" disabled={!uploadAllowed || mutation.isPending} type="submit">
        {t("imports.submit")}
      </button>
    </form>
  );
}

function JobRow({
  activeAction,
  isAdmin,
  job,
  onActionDone,
  onToggleAction,
}: {
  activeAction: ActiveAction;
  isAdmin: boolean;
  job: ImportJobListItem;
  onActionDone: () => void;
  onToggleAction: (kind: "errors" | "correction" | "rollback") => void;
}) {
  const { t } = useTranslation();
  const isActive = (kind: "errors" | "correction" | "rollback") =>
    activeAction?.jobId === job.id && activeAction.kind === kind;

  return (
    <>
      <tr>
        <th scope="row">{t(`imports.typeLabels.${job.importType}`, job.importType)}</th>
        <td>{job.originalFilename}</td>
        <td>{statusLabel(t, job.status)}</td>
        <td>
          {job.validRows}/{job.totalRows}
        </td>
        <td>{job.errorCount}</td>
        <td>{new Date(job.createdAt).toLocaleString("de-AT")}</td>
        <td className="action-cell">
          {job.errorCount > 0 ? (
            <button className="secondary-button" onClick={() => onToggleAction("errors")} type="button">
              {t("imports.actions.showErrors")}
            </button>
          ) : null}
          {isAdmin && job.status === "COMMITTED" ? (
            <>
              <button className="secondary-button" onClick={() => onToggleAction("correction")} type="button">
                {t("imports.actions.correct")}
              </button>
              <button className="secondary-button" onClick={() => onToggleAction("rollback")} type="button">
                {t("imports.actions.rollback")}
              </button>
            </>
          ) : null}
        </td>
      </tr>
      {isActive("errors") ? (
        <tr>
          <td colSpan={7}>
            <ErrorDetails errorOverflow={job.errorOverflow} jobId={job.id} />
          </td>
        </tr>
      ) : null}
      {isActive("correction") ? (
        <tr>
          <td colSpan={7}>
            <CorrectionForm jobId={job.id} onDone={onActionDone} />
          </td>
        </tr>
      ) : null}
      {isActive("rollback") ? (
        <tr>
          <td colSpan={7}>
            <RollbackForm jobId={job.id} onDone={onActionDone} />
          </td>
        </tr>
      ) : null}
    </>
  );
}

function statusLabel(t: (key: string) => string, status: JobStatus) {
  return t(`imports.status.${status}`);
}

function ErrorDetails({ errorOverflow, jobId }: { errorOverflow: boolean; jobId: string }) {
  const { t } = useTranslation();
  const query = useQuery({ queryKey: ["import-jobs", jobId, "errors"], queryFn: () => fetchErrors(jobId) });

  if (query.isPending) {
    return <p role="status">{t("imports.errorsPanel.loading")}</p>;
  }
  if (query.isError) {
    return (
      <p className="field-error" role="alert">
        {germanErrorMessage(query.error)}
      </p>
    );
  }

  return (
    <div className="panel">
      <h3>{t("imports.errorsPanel.title")}</h3>
      {errorOverflow ? <p role="note">{t("imports.errorsPanel.overflow")}</p> : null}
      <table className="data-table compact-table">
        <thead>
          <tr>
            <th>{t("imports.errorsPanel.row")}</th>
            <th>{t("imports.errorsPanel.column")}</th>
            <th>{t("imports.errorsPanel.value")}</th>
            <th>{t("imports.errorsPanel.message")}</th>
          </tr>
        </thead>
        <tbody>
          {(query.data?.items ?? []).map((error) => (
            <tr key={error.id}>
              <td>{error.rowNumber}</td>
              <td>{error.columnName}</td>
              <td>{error.rejectedValue ?? ""}</td>
              <td>{error.message}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CorrectionForm({ jobId, onDone }: { jobId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<CorrectionFormValues>();

  const mutation = useMutation({
    mutationFn: (values: CorrectionFormValues) => {
      const file = values.csvFile.item(0);
      if (!file) return Promise.reject(new Error("missing file"));
      return correctJob(jobId, file);
    },
    onSuccess: onDone,
  });

  return (
    <form className="form-panel" noValidate onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h3>{t("imports.correctionPanel.title")}</h3>
      <p>{t("imports.correctionPanel.description")}</p>
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
      {mutation.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(mutation.error)}
        </p>
      ) : null}
      <button className="primary-button" disabled={mutation.isPending} type="submit">
        {t("imports.correctionPanel.submit")}
      </button>
    </form>
  );
}

function RollbackForm({ jobId, onDone }: { jobId: string; onDone: () => void }) {
  const { t } = useTranslation();
  const {
    formState: { errors },
    handleSubmit,
    register,
  } = useForm<RollbackFormValues>();

  const mutation = useMutation({
    mutationFn: (values: RollbackFormValues) => rollbackJob(jobId, values.reason),
    onSuccess: onDone,
  });

  return (
    <form className="form-panel" noValidate onSubmit={handleSubmit((values) => mutation.mutate(values))}>
      <h3>{t("imports.rollbackPanel.title")}</h3>
      <p>{t("imports.rollbackPanel.description")}</p>
      <label className="field">
        <span>{t("imports.rollbackPanel.reason")}</span>
        <input
          type="text"
          {...register("reason", {
            required: t("imports.rollbackPanel.reasonRequired"),
            maxLength: { value: 500, message: t("imports.rollbackPanel.reasonRequired") },
            validate: (value) => value.trim().length > 0 || t("imports.rollbackPanel.reasonRequired"),
          })}
        />
      </label>
      {errors.reason?.message ? (
        <p className="field-error" role="alert">
          {errors.reason.message}
        </p>
      ) : null}
      {mutation.isError ? (
        <p className="field-error" role="alert">
          {germanErrorMessage(mutation.error)}
        </p>
      ) : null}
      <button className="primary-button" disabled={mutation.isPending} type="submit">
        {t("imports.rollbackPanel.submit")}
      </button>
    </form>
  );
}
