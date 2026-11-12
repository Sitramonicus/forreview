echo "---"; ls web/*.md 2>/dev/null; cat >> web/f_flares/NOTES.md <<'EOF'

## touched in the ui pass

- `PayrollPreviewUi.js` untouched, but the panel above it gained `.pay-switch` (MotorPH engine vs
  reference figures) and `#pay-mode-note`. The switch is copy only — it does not change the request.
