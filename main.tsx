import { Plugin, App, PluginSettingTab, Setting, Notice, MarkdownRenderer } from "obsidian";
import { evalCljs, main } from "saber.js"

// Reload comment


interface SaberSettings {
	nreplPort: string;
	replPort: string;
}

const DEFAULT_SETTINGS: SaberSettings = {
	nreplPort: '8703',
	replPort: '8080'
}


export default class Saber extends Plugin {

  settings: SaberSettings;

  async onload() {

    await this.loadSettings();

    this.addSettingTab(new SaberSettingTab(this.app, this));

    main(this);

    this.registerMarkdownCodeBlockProcessor("clojure-eval", (source, el, ctx) => {

      console.log(el.className);

      let result:string = evalCljs(source);
      let md:string = "```clojure\n" + result + "\n```\n\n";
      MarkdownRenderer.renderMarkdown(md, el, "", ctx);

    });


    // this.addCommand({"id": "repl", "name": "REPL", callback: () => {}});

    console.log('[:Saber :online]');

  }

  onunload() {

  }

  async loadSettings() {
    this.settings = Object.assign({}, DEFAULT_SETTINGS, await this.loadData());
  }


  async saveSettings() {
    await this.saveData(this.settings);
  }

}


class SaberSettingTab extends PluginSettingTab {
  plugin: Saber;

  constructor(app: App, plugin: Saber) {
    super(app, plugin);
    this.plugin = plugin;
  }

  display(): void {
    const {containerEl} = this;

    containerEl.empty();

    new Setting(containerEl)
      .setName('nREPL port')
      .addText( text => {
        text.setPlaceholder('8703')
          .setValue(this.plugin.settings.nreplPort)
          .onChange(async (value:string) => {
            this.plugin.settings.nreplPort = value;
            await this.plugin.saveSettings();
          }
                   )
      });

    new Setting(containerEl)
      .setName('REPL port')
      .addText( text => {
        text.setPlaceholder('8080')
          .setValue(this.plugin.settings.replPort)
          .onChange(async (value:string) => {
            this.plugin.settings.replPort = value;
            await this.plugin.saveSettings();
          }
                   )
      });
  }
}
