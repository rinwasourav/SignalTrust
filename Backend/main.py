from datetime import datetime
import hashlib
import hmac

from fastapi import Depends, FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sqlalchemy import Column, DateTime, Integer, String, Text
from sqlalchemy.orm import Session
import os

from database import Base, engine, get_db

app = FastAPI(
    title="SignalTrust API",
    description="Secure community-powered fraud detection hub.",
    version="1.1.0"
)

# Configuration (Use env vars for production)
SECRET = os.getenv("SIGNALTRUST_SECRET", "hackathon-security-secret-key")
ADMIN_API_KEY = "admin-secret-key-123"

async def verify_admin(x_admin_key: str = Header(None)):
    if x_admin_key != ADMIN_API_KEY:
        raise HTTPException(status_code=403, detail="Unauthorized: Invalid Admin Key")
    return x_admin_key

# Add CORS middleware to allow the dashboard to connect
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

SECRET = "change-this-demo-secret"

class Report(Base):
    __tablename__ = "reports"

    id = Column(Integer, primary_key=True, index=True)
    phone_token = Column(String(64), index=True, nullable=False)
    masked_number = Column(String(30), nullable=False)
    category = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    risk_score = Column(Integer, default=10)
    status = Column(String(30), default="PENDING")
    created_at = Column(DateTime, default=datetime.utcnow)

class ReportStatusRequest(BaseModel):
    status: str

class PhoneReputation(Base):
    __tablename__ = "phone_reputation"

    id = Column(Integer, primary_key=True, index=True)
    phone_token = Column(String(64), unique=True, index=True)
    masked_number = Column(String(30), nullable=False)
    risk_score = Column(Integer, default=0)
    report_count = Column(Integer, default=0)
    risk_level = Column(String(20), default="LOW")
    updated_at = Column(DateTime, default=datetime.utcnow)

class Connection(Base):
    __tablename__ = "connections"
    id = Column(Integer, primary_key=True, index=True)
    from_token = Column(String(64), index=True)
    to_token = Column(String(64), index=True)
    connection_type = Column(String(50))
    created_at = Column(DateTime, default=datetime.utcnow)

Base.metadata.create_all(bind=engine)

class ReportRequest(BaseModel):
    phone_number: str
    category: str
    description: str = None

def tokenise_number(number: str) -> str:
    return hmac.new(
        SECRET.encode(),
        number.encode(),
        hashlib.sha256
    ).hexdigest()

def mask_number(number: str) -> str:
    digits = "".join(ch for ch in number if ch.isdigit())
    if len(digits) < 4:
        return digits
    return "****" + digits[-4:]

def calculate_level(score: int) -> str:
    if score >= 75:
        return "HIGH"
    if score >= 40:
        return "MEDIUM"
    return "LOW"

@app.get("/health")
def health():
    return {"status": "ok", "service": "SignalTrust API"}

@app.post("/reports")
def create_report(
    request: ReportRequest,
    db: Session = Depends(get_db)
):
    number = request.phone_number.strip()
    token = tokenise_number(number)
    masked = mask_number(number)

    reputation = (
        db.query(PhoneReputation)
        .filter(PhoneReputation.phone_token == token)
        .first()
    )

    if reputation is None:
        reputation = PhoneReputation(
            phone_token=token,
            masked_number=masked,
            risk_score=10,
            report_count=0,
            risk_level="LOW"
        )
        db.add(reputation)

    reputation.report_count += 1
    reputation.risk_score = min(100, reputation.risk_score + 10)
    reputation.risk_level = calculate_level(reputation.risk_score)
    reputation.updated_at = datetime.utcnow()

    report = Report(
        phone_token=token,
        masked_number=masked,
        category=request.category,
        description=request.description,
        risk_score=reputation.risk_score
    )

    db.add(report)
    db.commit()
    db.refresh(report)

    return {
        "message": "Report saved",
        "report_id": report.id,
        "masked_number": masked,
        "risk_score": reputation.risk_score,
        "risk_level": reputation.risk_level,
        "report_count": reputation.report_count
    }

@app.get("/dashboard/stats")
def dashboard_stats(db: Session = Depends(get_db)):
    total_reports = db.query(Report).count()
    high_risk = db.query(PhoneReputation).filter(PhoneReputation.risk_level == "HIGH").count()
    medium_risk = db.query(PhoneReputation).filter(PhoneReputation.risk_level == "MEDIUM").count()
    low_risk = db.query(PhoneReputation).filter(PhoneReputation.risk_level == "LOW").count()
    campaigns = db.query(PhoneReputation).filter(PhoneReputation.report_count >= 3).count()

    return {
        "total_reports": total_reports,
        "high_risk_numbers": high_risk,
        "medium_risk_numbers": medium_risk,
        "low_risk_numbers": low_risk,
        "fraud_campaigns": campaigns
    }

@app.get("/dashboard/reports")
def dashboard_reports(db: Session = Depends(get_db)):
    reports = (
        db.query(Report)
        .order_by(Report.created_at.desc())
        .limit(100)
        .all()
    )

    return [
        {
            "id": item.id,
            "number": item.masked_number,
            "category": item.category,
            "risk_score": item.risk_score,
            "status": item.status,
            "created_at": item.created_at.isoformat()
        }
        for item in reports
    ]

@app.patch("/admin/reports/{report_id}/status", dependencies=[Depends(verify_admin)])
def update_report_status(
    report_id: int,
    request: ReportStatusRequest,
    db: Session = Depends(get_db)
):
    report = db.query(Report).filter(Report.id == report_id).first()
    if report is None:
        return {"error": "Report not found"}

    allowed = {"PENDING", "CONFIRMED", "FALSE_POSITIVE", "OUTDATED"}
    if request.status.upper() not in allowed:
        return {"error": "Invalid status"}

    report.status = request.status.upper()
    db.commit()
    return {"message": "Report status updated", "status": report.status}

@app.get("/reputation")
def reputation(
    phone_number: str,
    db: Session = Depends(get_db)
):
    token = tokenise_number(phone_number)
    item = db.query(PhoneReputation).filter(PhoneReputation.phone_token == token).first()

    if item is None:
        return {
            "phone_number": mask_number(phone_number),
            "risk_score": 0,
            "risk_level": "LOW",
            "report_count": 0
        }

    return {
        "phone_number": item.masked_number,
        "risk_score": item.risk_score,
        "risk_level": item.risk_level,
        "report_count": item.report_count
    }

@app.get("/dashboard/graph")
def dashboard_graph(db: Session = Depends(get_db)):
    reputations = db.query(PhoneReputation).all()
    connections = db.query(Connection).all()

    nodes = []
    for rep in reputations:
        nodes.append({
            "id": rep.phone_token,
            "label": rep.masked_number,
            "risk": rep.risk_level,
            "score": rep.risk_score
        })

    edges = []
    for conn in connections:
        edges.append({
            "from": conn.from_token,
            "to": conn.to_token,
            "type": conn.connection_type
        })

    return {"nodes": nodes, "edges": edges}
