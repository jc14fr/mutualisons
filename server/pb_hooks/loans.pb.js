// Création d'un prêt : le prêteur est déduit du propriétaire de l'objet.
// L'état "prêté" n'est plus écrit dans items.status (il est calculé par item-state).
onRecordCreateRequest((e) => {
    let item;
    try {
        item = $app.findRecordById("items", e.record.getString("item"));
    } catch (err) {
        throw new BadRequestError("Objet introuvable.");
    }

    if (!e.hasSuperuserAuth()) {
        if (!e.auth || item.getString("owner") !== e.auth.id) {
            throw new BadRequestError("Vous ne pouvez prêter que vos objets.");
        }
    }

    e.record.set("lender", item.getString("owner"));
    e.next();
}, "loans");