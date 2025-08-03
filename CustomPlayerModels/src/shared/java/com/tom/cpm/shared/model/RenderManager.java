package com.tom.cpm.shared.model;

import java.util.function.Function;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import com.tom.cpl.text.FormatText;
import com.tom.cpm.shared.animation.AnimationEngine.AnimationMode;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.definition.ModelDefinitionLoader;
import com.tom.cpm.shared.model.render.ModelRenderManager;

public class RenderManager<G, P, M, D> {
	private Player<P> profile;
	private final ModelRenderManager<D, ?, ?, M> renderManager;
	private final ModelDefinitionLoader<G> loader;
	private Function<P, G> getProfile;
	private Function<G, String> getSkullModel;
	private Function<G, String> getTexture;

	public RenderManager(ModelRenderManager<D, ?, ?, M> renderManager,
			ModelDefinitionLoader<G> loader, Function<P, G> getProfile) {
		this.renderManager = renderManager;
		this.loader = loader;
		this.getProfile = getProfile;
	}

	@SuppressWarnings("unchecked")
	public boolean tryBindModel(G gprofile, P player, D buffer, M toBind, String arg, String unique, AnimationMode mode) {
		if(gprofile == null)gprofile = getProfile.apply(player);
		Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
		if(profile == null) {
			renderManager.unbindModel(toBind);
			return false;
		}
		if (!profile.isModel && getTexture != null) {
			String texture = getTexture.apply(gprofile);
			String ptexture = getTexture.apply((G) profile.getGameProfile());
			if (texture != null && !Objects.equal(texture, ptexture)) {
				profile = (Player<P>) loader.reloadPlayer(gprofile, unique);
			}
			if(profile == null) {
				renderManager.unbindModel(toBind);
				return false;
			}
		}
		ModelDefinition def = profile.getModelDefinition();
		if(def != null) {
			this.profile = profile;
			profile.animState.animationMode = mode;
			if(player != null)
				profile.updatePlayer(player);
			def.itemTransforms.clear();
			renderManager.bindModel(toBind, arg, buffer, def, profile, profile.animState.animationMode);
			renderManager.getAnimationEngine().prepareAnimations(profile, profile.animState.animationMode, def);
			return true;
		}
		renderManager.unbindModel(toBind);
		return false;
	}

	@SuppressWarnings("unchecked")
	public Player<P> loadPlayerState(G gprofile, P player, String unique, AnimationMode mode) {
		if(gprofile == null)gprofile = getProfile.apply(player);
		Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
		if(profile == null) {
			return null;
		}
		ModelDefinition def = profile.getModelDefinition();
		if(def != null) {
			this.profile = profile;
			profile.animState.animationMode = mode;
			if(player != null)
				profile.updatePlayer(player);
			def.itemTransforms.clear();
			return profile;
		}
		return null;
	}

	public void bindPlayerState(Player<P> player, D buffer, M toBind, String arg) {
		if (profile != null) {
			ModelDefinition def = profile.getModelDefinition();
			renderManager.bindModel(toBind, arg, buffer, def, profile, profile.animState.animationMode);
			renderManager.getAnimationEngine().prepareAnimations(profile, profile.animState.animationMode, def);
		}
	}

	public void unbindClear(M model) {
		unbindFlush(model);
		clearBoundPlayer();
	}

	public void unbind(M model) {
		renderManager.unbindModel(model);
	}

	public void unbindFlush(M model) {
		renderManager.flushBatch(model, null);
		unbind(model);
	}

	public void bindHand(P player, D buffer, M model) {
		tryBindModel(null, player, buffer, model, null, ModelDefinitionLoader.PLAYER_UNIQUE, AnimationMode.HAND);
	}

	public void bindSkull(G profile, D buffer, M model) {
		Player<P> prev = this.profile;
		String unique;
		if(getSkullModel == null)unique = ModelDefinitionLoader.SKULL_UNIQUE;
		else {
			unique = getSkullModel.apply(profile);
			if(unique == null) {
				if(getTexture == null)unique = ModelDefinitionLoader.SKULL_UNIQUE;
				else unique = getTexture.apply(profile);
				if(unique == null)unique = ModelDefinitionLoader.SKULL_UNIQUE;
				else unique = "skull_tex:" + unique;
			}
			else unique = "model:" + unique;
		}
		tryBindModel(profile, null, buffer, model, null, unique, AnimationMode.SKULL);
		this.profile = prev;
	}

	public void bindPlayer(P player, D buffer, M model) {
		if(profile != null)clearBoundPlayer();
		tryBindModel(null, player, buffer, model, null, ModelDefinitionLoader.PLAYER_UNIQUE, AnimationMode.PLAYER);
	}

	public void bindArmor(M player, M model, int layer) {
		renderManager.bindSubModel(player, model, "armor" + layer);
	}

	public void bindElytra(M player, M model) {
		renderManager.bindSubModel(player, model, null);
	}

	public void bindSkin(M model, TextureSheetType tex) {
		renderManager.bindSkin(model, null, tex);
	}

	public Player<P> getBoundPlayer() {
		return profile;
	}

	public void clearBoundPlayer() {
		profile = null;
	}

	public void setGetSkullModel(Function<G, String> getSkullModel) {
		this.getSkullModel = getSkullModel;
	}

	public void setGetTexture(Function<G, String> getTexture) {
		this.getTexture = getTexture;
	}

	public <PR> void setGPGetters(Function<G, Multimap<String, PR>> getMap, Function<PR, String> getValue) {
		setGetSkullModel(profile -> {
			PR property = Iterables.getFirst(getMap.apply(profile).get("cpm:model"), null);
			if(property != null)return getValue.apply(property);
			return null;
		});
		setGetTexture(profile -> {
			PR property = Iterables.getFirst(getMap.apply(profile).get("textures"), null);
			if(property != null)return getValue.apply(property);
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	public void jump(P player) {
		G gprofile = getProfile.apply(player);
		Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, ModelDefinitionLoader.PLAYER_UNIQUE);
		if(profile == null)return;
		profile.animState.jump();
	}

	@SuppressWarnings("unchecked")
	public FormatText getStatus(G gprofile, String unique) {
		Player<P> profile = (Player<P>) loader.loadPlayer(gprofile, unique);
		if(profile == null)return null;
		ModelDefinition def = profile.getModelDefinition0();
		return def != null ? def.getStatus() : null;
	}
}
