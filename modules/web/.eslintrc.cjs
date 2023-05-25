module.exports = {
  root: true,
  env: {
    browser: true,
    es2021: true,
    node: true,
  },
  extends: [
    "plugin:vue/vue3-essential",
    "eslint:recommended",
    "plugin:prettier/recommended",
    "./.eslintrc-auto-import.json",
  ],
  parserOptions: {
    ecmaVersion: "latest",
  },
  rules: {
    "no-unused-vars": "warn",
  },
};
