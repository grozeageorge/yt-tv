import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import express from "express";
import cors from "cors";
import sql from "mssql";

const app = express();
app.use(cors());

const server = new Server(
  {
    name: "yt-tv-sql-server",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Database configuration
const dbConfig: sql.config = {
  user: process.env.DB_USERNAME || "sa",
  password: process.env.DB_PASSWORD || "sapassword",
  database: process.env.DB_NAME || "yt_tv",
  server: process.env.DB_HOST || "localhost",
  port: parseInt(process.env.DB_PORT || "1433"),
  options: {
    encrypt: false, // For local dev
    trustServerCertificate: true,
  },
};

async function executeQuery(query: string) {
  try {
    const pool = await sql.connect(dbConfig);
    const result = await pool.request().query(query);
    return result.recordset;
  } catch (err) {
    console.error("Database error:", err);
    throw err;
  }
}

/**
 * Execute a parameterized scalar COUNT query for playlists by user id.
 * Returns the integer count.
 */
async function countPlaylistsForUser(userId: number) {
  try {
    const pool = await sql.connect(dbConfig);
    const request = pool.request();
    // Use BigInt to support large user ids; fall back to Int if needed by your schema
    request.input("userId", sql.BigInt, userId);
    const result = await request.query("SELECT COUNT(*) AS count FROM playlists WHERE user_id = @userId");
    const row = result.recordset && result.recordset[0];
    return row ? parseInt(row.count, 10) || 0 : 0;
  } catch (err) {
    console.error("Database error (countPlaylistsForUser):", err);
    throw err;
  }
}

// Register tools
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
      tools: [
      {
        name: "list_tables",
        description: "List all tables in the database",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
      {
        name: "describe_table",
        description: "Get schema information for a specific table",
        inputSchema: {
          type: "object",
          properties: {
            table: { type: "string" },
          },
          required: ["table"],
        },
      },
        {
          name: "count_user_playlists",
          description: "Return the number of playlists owned by a specific user. Requires 'userId' in the input schema.",
          inputSchema: {
            type: "object",
            properties: {
              userId: { type: "number" }
            },
            required: ["userId"]
          }
        },
      {
        name: "execute_query",
        description: "Execute a read-only SQL query on the database",
        inputSchema: {
          type: "object",
          properties: {
            sql: { type: "string" },
          },
          required: ["sql"],
        },
      },
    ],
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    switch (name) {
      case "list_tables": {
        const result = await executeQuery(
          "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
        );
        return {
          content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
        };
      }
      case "describe_table": {
        const table = args?.table as string;
        const result = await executeQuery(
          `SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '${table}'`
        );
        return {
          content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
        };
      }
      case "count_user_playlists": {
        // Ensure we have a userId parameter and it is a number
        const userIdRaw = args?.userId;
        if (userIdRaw === undefined || userIdRaw === null) {
          throw new Error("Missing required parameter 'userId'");
        }

        const userId = Number(userIdRaw);
        if (Number.isNaN(userId)) {
          throw new Error("Parameter 'userId' must be a number");
        }

        // Execute parameterized COUNT query and return a simple JSON result
        const count = await countPlaylistsForUser(userId);
        return {
          content: [{ type: "text", text: JSON.stringify({ count }) }],
        };
      }
      case "execute_query": {
        const query = args?.sql as string;
        // Basic check for read-only (not foolproof but better than nothing)
        if (!query.toLowerCase().trim().startsWith("select")) {
            throw new Error("Only SELECT queries are allowed via this tool.");
        }
        const result = await executeQuery(query);
        return {
          content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
        };
      }
      default:
        throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error: any) {
    return {
      content: [{ type: "text", text: `Error: ${error.message}` }],
      isError: true,
    };
  }
});

let transport: SSEServerTransport | null = null;

app.get("/health", (req, res) => {
  res.send("OK");
});

app.get("/sse", async (req, res) => {
  console.log("New SSE connection");
  transport = new SSEServerTransport("/messages", res);
  await server.connect(transport);
});

app.post("/messages", async (req, res) => {
  console.log("New message received");
  if (transport) {
    await transport.handlePostMessage(req, res);
  } else {
    res.status(400).send("No active SSE transport");
  }
});

const PORT = process.env.MCP_PORT || 3000;
app.listen(PORT, () => {
  console.log(`MCP Server running on http://localhost:${PORT}`);
});
