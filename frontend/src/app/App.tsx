import { useState } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider, type RouterProviderProps } from "react-router";

import "./i18n";
import { createQueryClient } from "./queryClient";
import { createAppRouter } from "./routes";

type AppProps = {
  router?: RouterProviderProps["router"];
};

export function App({ router: providedRouter }: AppProps) {
  const [queryClient] = useState(() => createQueryClient());
  const [router] = useState(() => providedRouter ?? createAppRouter());

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  );
}
