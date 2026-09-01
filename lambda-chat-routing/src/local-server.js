// Local dev only. Thin wrapper so handler() can be called with a plain curl, no AWS Lambda
// infra needed. docker-compose overrides the command to run this instead of the Dockerfile's
// default CMD (lambda-entry.js).
import express from 'express';
import { handler } from './handler.js';

const app = express();
const port = process.env.PORT || 8080;

app.get('/connect-info', async (req, res) => {
  const result = await handler({ queryStringParameters: req.query });

  res.status(result.statusCode);
  for (const [key, value] of Object.entries(result.headers ?? {})) {
    res.set(key, value);
  }
  res.send(result.body);
});

app.listen(port, () => {
  console.log(`Local chat-routing server listening on port ${port}`);
});
