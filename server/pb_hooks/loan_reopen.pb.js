routerAdd("POST", "/api/mutualisons/reopen", (e) => {
    const loanId = (e.requestInfo().body.loan || "").trim();
    if (!loanId) throw new BadRequestError("Prêt manquant.");

    let loan;
    try {
        loan = $app.findRecordById("loans", loanId);
    } catch (err) {
        throw new BadRequestError("Prêt introuvable.");
    }

    if (loan.getString("lender") !== e.auth.id) {
        throw new BadRequestError("Seul le prêteur peut rouvrir ce prêt.");
    }
    if (loan.getString("returned_at") === "") {
        throw new BadRequestError("Ce prêt n'est pas marqué rendu.");
    }

    loan.set("returned_at", "");
    $app.save(loan);

    return e.json(200, { ok: true });
}, $apis.requireAuth());