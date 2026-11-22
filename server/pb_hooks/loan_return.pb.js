routerAdd("POST", "/api/mutualisons/return", (e) => {
    const loanId = (e.requestInfo().body.loan || "").trim();
    if (!loanId) throw new BadRequestError("Prêt manquant.");

    let loan;
    try {
        loan = $app.findRecordById("loans", loanId);
    } catch (err) {
        throw new BadRequestError("Prêt introuvable.");
    }

    let item = null;
    try {
        item = $app.findRecordById("items", loan.getString("item"));
    } catch (err) {
        item = null;
    }

    const estPreteur = loan.getString("lender") === e.auth.id;
    const estProprietaire = item !== null && item.getString("owner") === e.auth.id;
    if (!estPreteur && !estProprietaire) {
        throw new BadRequestError("Seul le propriétaire de l'objet peut marquer ce prêt rendu.");
    }
    if (loan.getString("returned_at") !== "") {
        throw new BadRequestError("Ce prêt est déjà marqué rendu.");
    }

    loan.set("returned_at", new Date().toISOString().replace("T", " "));
    $app.save(loan);

    return e.json(200, { ok: true });
}, $apis.requireAuth());