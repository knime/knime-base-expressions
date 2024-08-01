import "vitest-canvas-mock";
import { Consola, LogLevel } from "consola";

export const consola = new Consola({
  level: LogLevel.Log,
});

// @ts-expect-error
window.consola = consola;
