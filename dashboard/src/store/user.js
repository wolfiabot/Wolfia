/**
 * Represents a single Discord User.
 */
export class User {
	constructor(discordId, name, avatarId, discriminator) {
		this.discordId = discordId;
		this.name = name;
		this.avatarId = avatarId;
		this.discriminator = discriminator;
	}

	avatarUrl() {
		if (this.avatarId === null || this.avatarId === "") {
			let number = this.discriminator % 5;
			return `https://cdn.discordapp.com/embed/avatars/${number}.png`;
		}
		const ext = this.avatarId.startsWith("a_") ? "gif" : "png";
		return `https://cdn.discordapp.com/avatars/${this.discordId}/${this.avatarId}.${ext}`;
	}
}
