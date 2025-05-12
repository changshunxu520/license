package com.lemonzuo.license.autopatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@RestController
@RequestMapping("/plugins")
@Slf4j
public class AutoPatchController {
	private static final String DEFAULT_CONFIG_PATH = "plugins/plugins.json";
	/**
	 * JetBrains 插件仓库服务
	 * IDE 访问此接口获取插件列表
	 */
	@GetMapping("/plugins.xml")
	public void pluginsXml(HttpServletResponse response) throws Exception {
		response.setContentType("application/xml;charset=UTF-8");
		// 获取运行时工作目录
		String currentDir = System.getProperty("user.dir");
		String configPath = System.getProperty("plugins.config.path", DEFAULT_CONFIG_PATH);
		File configFile = new File(currentDir, configPath);
		// 检查文件是否存在
		if (!configFile.exists()) {
			log.error("配置文件不存在: {}", configFile.getAbsolutePath());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try (PrintWriter writer = response.getWriter()) {
				writer.write("Configuration file not found: " + configFile.getAbsolutePath());
				writer.flush();
			}
			return;
		}
		// 读取 JSON 文件
		ObjectMapper objectMapper = new ObjectMapper();
		List<PluginInfo> plugins = objectMapper.readValue(configFile, new TypeReference<>() {});

		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		// 拼接 URL
		for (PluginInfo plugin : plugins) {
			plugin.setUrl(baseUrl + plugin.getUrl());
		}
		String xmlContent = getPluginsXml(plugins);
		// 写入响应
		try (PrintWriter writer = response.getWriter()) {
			writer.write(xmlContent);
			writer.flush();
		}
	}
	/**
	 * 获取插件列表的 XML 内容
	 * @Author xuchangshun
	 * @param plugins :  插件列表
	 * @return java.lang.String
	 * @Date 2025/5/12 18:19
	 */
	private static String getPluginsXml(List<PluginInfo> plugins) {
		StringBuilder xmlContent = new StringBuilder();
		xmlContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plugins>\n");
		for (PluginInfo plugin : plugins) {
			xmlContent.append(String.format(
					"""
					<plugin id="cn.maoxian.xucs.AutoPatchDirGenerate" url="%s" version="%s">
						<name>AutoPatchDirGenerate</name>
						<description>宏景自动补丁生成工具 - %s</description>
						<vendor email="xucs@hjsoft.com.cn" url="https://www.hjsoft.com.cn">宏景软件</vendor>
						<idea-version since-build="%s" until-build="%s"/>
					</plugin>\n
					""",
					plugin.url, plugin.version, plugin.version, plugin.sinceBuild, plugin.untilBuild
			));
		}
		xmlContent.append("</plugins>");
		return xmlContent.toString();
	}

	/**
	 * JetBrains 插件仓库服务
	 * IDE 访问此接口获取插件列表
	 */
	@GetMapping("autopatch/{fileName:.+}")
	public void downloadPlugin(@PathVariable String fileName, HttpServletResponse response) throws Exception {
		// 获取当前运行的工作目录
		String jarPath = System.getProperty("user.dir");
		log.info("Working directory: {}", jarPath);
		log.info("jarPath: {}", jarPath);
		File file = new File(jarPath, "plugins/autopatch/"+fileName);
		if (!file.exists()) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename="+fileName);
		try (var in = new java.io.FileInputStream(file);
		     var out = response.getOutputStream()) {
			in.transferTo(out);
		}
	}
	// 内部类用于存储插件信息
	// 插件信息内部类
	@Setter
	@Getter
	static class PluginInfo {
		// Getters 和 Setters
		private String version;
		private String url;
		private String sinceBuild;
		private String untilBuild;

	}

}
