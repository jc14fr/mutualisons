onRecordCreateRequest((e) => {
    // Le compte qui crée le cercle en devient le premier membre
    e.record.set("members", [e.auth.id]);
    e.next();
}, "circles");

onRecordUpdateRequest((e) => {
    const avant = e.record.original().getStringSlice("members").slice().sort().join(",");
    const apres = e.record.getStringSlice("members").slice().sort().join(",");
    if (avant !== apres) {
        throw new BadRequestError("La liste des membres ne se modifie pas directement.");
    }
    e.next();
}, "circles");