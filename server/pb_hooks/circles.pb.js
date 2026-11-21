onRecordCreateRequest((e) => {
    if (e.auth && !e.hasSuperuserAuth()) {
        e.record.set("members", [e.auth.id]);
        e.record.set("creator", e.auth.id);
    }
    e.next();
}, "circles");

onRecordUpdateRequest((e) => {
    if (!e.hasSuperuserAuth()) {
        // Le créateur ne change jamais
        if (e.record.original().get("creator") !== e.record.get("creator")) {
            throw new BadRequestError("Le créateur d'un cercle ne peut pas être modifié.");
        }
    }
    const avant = e.record.original().getStringSlice("members").slice().sort().join(",");
    const apres = e.record.getStringSlice("members").slice().sort().join(",");
    if (avant !== apres && !e.hasSuperuserAuth()) {
        throw new BadRequestError("La liste des membres ne se modifie pas directement.");
    }
    e.next();
}, "circles");