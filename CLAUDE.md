# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

Siehe [README.md](README.md) für das, was dieser Prüfstand tut und wie ein Projekt ihn einbindet: Szenario-Datei, `ScenarioLayout`, `ScenarioContext`, die Ports, die er ersetzt, und die, die er bewusst strenger fasst als die Produktion.

## Grundsatz

Der Prüfstand darf **nie mehr können als die Produktion**. Jede Stelle, an der ein Szenario etwas liefert, was der IES-Adapter nicht liefern würde, ist eine Fehlerklasse, die erst der Publish findet. Eine Lücke der Produktion wird im Prüfstand nachgebildet, nicht geschlossen.
