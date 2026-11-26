export default async function playTitle(stone) {
  const { titulo, idPista, volumen, ecualizador, segundoPlano } = await stone.pedido();
  const steps = {};
  const result = (status, extra = {}) => ({ status, segundoPlano, steps, ...extra });
  const normalize = (s) => s.normalize("NFD").replace(/\p{M}+/gu, "").trim().toLowerCase().replace(/\s+/g, " ");
  const title = normalize(titulo);
  if (!title) {
    steps.search = { code: "INVALID_PARAM" };
    return result("failed");
  }
  const search = await stone.buscar();
  if (search.error || !Array.isArray(search.results) || !Number.isInteger(search.total) || search.total < search.results.length) {
    steps.search = { code: search.error || "SEARCH_NOT_CONFIRMED" };
    return result("failed");
  }
  const complete = search.total === search.results.length;
  const songs = new Map();
  for (const song of search.results) {
    if (song && song.isVideo === false && typeof song.id === "string" && song.id &&
        typeof song.title === "string" && normalize(song.title).includes(title)) {
      songs.set(song.id, { id: song.id, title: song.title, artist: song.artist || "", album: song.album || "" });
    }
  }
  const matches = [...songs.values()];
  const exact = matches.filter((song) => normalize(song.title) === title);
  const candidates = complete && exact.length ? exact : matches;
  const selected = idPista ? candidates.find((song) => song.id === idPista) :
    complete && candidates.length === 1 ? candidates[0] : undefined;
  if (!selected) {
    if (idPista || candidates.length === 0) {
      steps.search = { code: idPista ? "INVALID_SELECTION" : complete ? "TRACK_NOT_FOUND" : "SEARCH_INCOMPLETE" };
      return result("failed");
    }
    steps.search = { code: "AMBIGUOUS_TITLE" };
    return result("ambiguous", { matches: candidates });
  }
  steps.search = { code: "OK" };
  if (volumen !== undefined) {
    const response = await stone.volumen();
    if (response.ok !== true) {
      steps.volume = { code: response.error || "VOLUME_NOT_CONFIRMED" };
      return result("failed", { selected });
    }
    steps.volume = { code: "OK" };
  }
  if (ecualizador !== undefined) {
    const response = await stone.ecualizador();
    steps.equalizer = { code: response.ok === true ? "OK" : response.error || "EQ_NOT_CONFIRMED" };
  }
  const play = await stone.reproducir(selected.id);
  if (play.ok !== true) {
    steps.play = { code: play.error || "PLAY_NOT_CONFIRMED" };
    return result("failed");
  }
  // Only readbacks can be repeated after a successful write.
  let state;
  for (let attempt = 0; attempt < 3; attempt++) {
    if (attempt) await new Promise((resolve) => setTimeout(resolve, 180));
    state = await stone.estado();
    if (state.error || (state.playing === true && state.songId === selected.id)) break;
  }
  if (state?.playing !== true || state?.songId !== selected.id) {
    steps.play = { code: state?.error || "PLAY_NOT_CONFIRMED" };
    return result("failed");
  }
  steps.play = { code: "OK" };
  if (steps.volume && Number.isInteger(state.volumePercent)) steps.volume.actual = state.volumePercent;
  return result(steps.equalizer && steps.equalizer.code !== "OK" ? "partial" : "playing", { track: selected });
}
