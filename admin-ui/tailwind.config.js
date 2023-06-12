/** @type {import('tailwindcss').Config} */
// import defaultTheme from '@tailwindcss'
const defaultTheme = require('tailwindcss/defaultTheme')
// const defaultTheme = require(defaultTheme')

module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter var', ...defaultTheme.fontFamily.sans],
      },
    },
  },
  plugins: [],
}

