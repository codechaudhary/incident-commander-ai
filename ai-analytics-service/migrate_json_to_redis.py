import asyncio
import json
from pathlib import Path
from redis.asyncio import Redis

async def main():
    json_path = Path("data/ai_analysis.json")
    if not json_path.exists():
        print(f"No file found at {json_path}")
        return

    with open(json_path, "r", encoding="utf-8") as f:
        try:
            records = json.load(f)
        except json.JSONDecodeError:
            print("Invalid JSON")
            return

    client = Redis.from_url("redis://localhost:6379", decode_responses=True)
    
    count = 0
    for record in records:
        analysis_id = record["analysis_id"]
        trace_id = record["trace_id"]
        
        # Save to Redis mimicking the RedisAnalysisRepository logic
        await client.set(f"ai_analysis:{analysis_id}", json.dumps(record))
        await client.set(f"trace_to_analysis:{trace_id}", analysis_id)
        count += 1
        
    print(f"Migrated {count} records from JSON to Redis.")
    await client.aclose()

if __name__ == "__main__":
    asyncio.run(main())
