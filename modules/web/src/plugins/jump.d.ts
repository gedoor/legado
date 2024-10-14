export {}
export type Options = {
  duration?: number | [(distance: number) => number]
  offset?: number
  callback?: () => void // "undefined" is a suitable default, and won't be called
  easing?: (
    timeElapsed: number,
    start: number,
    distance: number,
    duration: number,
  ) => number
  a11y?: boolean
  container?: HTMLElement | string
}
export default function (
  target: number | string | HTMLElement,
  options: Options = {},
): void
