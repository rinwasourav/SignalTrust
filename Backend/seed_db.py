import hashlib
import hmac
import random
from datetime import datetime, timedelta
from database import SessionLocal, Base, engine
from main import PhoneReputation, Report, Connection, SECRET

def tokenise_number(number: str) -> str:
    return hmac.new(
        SECRET.encode(),
        number.encode(),
        hashlib.sha256
    ).hexdigest()

def mask_number(number: str) -> str:
    return "****" + number[-4:]

def calculate_level(score: int) -> str:
    if score >= 75:
        return "HIGH"
    if score >= 40:
        return "MEDIUM"
    return "LOW"

def seed():
    db = SessionLocal()

    # Clear existing data
    db.query(Report).delete()
    db.query(Connection).delete()
    db.query(PhoneReputation).delete()
    db.commit()

    print("Seeding 50 phone numbers...")

    phone_numbers = []
    tokens = []

    # Generate 50 numbers
    for i in range(50):
        num = f"900000{1000 + i}"
        phone_numbers.append(num)
        token = tokenise_number(num)
        tokens.append(token)

        score = random.randint(0, 95)
        count = random.randint(0, 15) if score > 20 else 0

        rep = PhoneReputation(
            phone_token=token,
            masked_number=mask_number(num),
            risk_score=score,
            report_count=count,
            risk_level=calculate_level(score)
        )
        db.add(rep)

        # Add some reports for high/medium risk numbers
        if score > 30:
            categories = ["OTP fraud", "Spam", "Bank impersonation", "Delivery scam"]
            for _ in range(random.randint(1, 3)):
                report = Report(
                    phone_token=token,
                    masked_number=mask_number(num),
                    category=random.choice(categories),
                    description="Automated sample report for demonstration.",
                    risk_score=score,
                    created_at=datetime.utcnow() - timedelta(days=random.randint(0, 5))
                )
                db.add(report)

    print("Seeding connections for contact graph...")

    # Create connections (Fraud clusters)
    # Connect some high-risk numbers together
    high_risk_tokens = [t for i, t in enumerate(tokens) if i < 10] # first 10 as potential cluster
    for i in range(len(high_risk_tokens) - 1):
        conn = Connection(
            from_token=high_risk_tokens[i],
            to_token=high_risk_tokens[i+1],
            connection_type="FREQUENT_CALL"
        )
        db.add(conn)

    # Connect a few random ones
    for _ in range(15):
        db.add(Connection(
            from_token=random.choice(tokens),
            to_token=random.choice(tokens),
            connection_type="SMS_LINK"
        ))

    db.commit()
    db.close()
    print("Database seeded successfully.")

if __name__ == "__main__":
    seed()
