# t_tkt

Leave, overtime, tickets.

- `WorkflowService` holds the rules; the store just persists.
- Errors here are read by people, not devs. If you put an employee id or a "demo" word in a
  message, it shows up in the UI forever. Two shipped that way already.
- `InMemoryWorkflowStore` is for tests. `FirestoreWorkflowStore` is the live one when Firebase is
  configured.
- `FirestoreSeedService` only works against Firestore. Local mode has no seed route.
