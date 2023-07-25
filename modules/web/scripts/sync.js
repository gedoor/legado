import { URL } from "node:url";
import fs from "node:fs";

const LEGADO_ASSETS_WEB_VUE_DIR = new URL(
  "../../../app/src/main/assets/web/vue",
  import.meta.url,
);
const VUE_DIST_DIR = new URL("../dist", import.meta.url);

console.log("> delete", LEGADO_ASSETS_WEB_VUE_DIR.pathname);
// 删除
fs.rm(
  LEGADO_ASSETS_WEB_VUE_DIR,
  {
    force: true,
    recursive: true,
  },
  (error) => {
    if (error) console.log(error);
    console.log("> mkdir", LEGADO_ASSETS_WEB_VUE_DIR.pathname);
    fs.mkdir(LEGADO_ASSETS_WEB_VUE_DIR, (error) => {
      if (error) return console.error(error);
      console.log("> cp dist files");
      fs.cp(
        VUE_DIST_DIR,
        LEGADO_ASSETS_WEB_VUE_DIR,
        {
          recursive: true,
        },
        (error) => {
          if (error) {
            console.warn("> cp error, you may copy files yourshelf");
            throw error;
          }
          console.log("> cp success");
        },
      );
    });
  },
);
