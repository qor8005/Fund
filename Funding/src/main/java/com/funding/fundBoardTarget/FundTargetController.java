package com.funding.fundBoardTarget;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.funding.Categorie.Categorie;
import com.funding.Categorie.CategorieService;
import com.funding.alert.AlertService;
import com.funding.answer.Answer;
import com.funding.answer.AnswerService;
import com.funding.cancels.CancelsController;
import com.funding.cancels.CancelsService;
import com.funding.file.FileService;
import com.funding.fundTargetList.FundTargetList;
import com.funding.fundTargetList.FundTargetListService;
import com.funding.fundUser.FundUser;
import com.funding.fundUser.FundUserService;
import com.funding.sale.Sale;
import com.funding.sale.SaleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/fundTarget")
@RequiredArgsConstructor
@Controller
public class FundTargetController {

	private final FundTargetService fundTargetService;
	private final FundTargetListService fundTargetListService;
	private final CategorieService categorieService;
	private final AnswerService answerService;
	private final FileService fileService;
	private final FundUserService fundUserService;
	private final AlertService alertService;
	private final SaleRepository saleRepository;
	private final CancelsController cancelsController;
	private final CancelsService cancelsService;

	
	
	//??? ????????? ????????????
	@GetMapping("/form")
	private String form(TargetForm targetForm, Model model,Principal principal) {
		List<Categorie> cList = categorieService.findAll();
		
		//?????? ?????? ??????(????????? ?????? ?????? ????????? ????????? ?????????)
		if(principal != null) {
			
			log.info("?????? ?????? ?????? :" + principal.getName());
			Optional<FundUser> user = fundUserService.findByuserName(principal.getName());
			if(user.isEmpty()) {
				return "/fundTarget/notUser";
			}
		}
		
		
		String nowTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		
		model.addAttribute("cList",cList);
		model.addAttribute("nowTime", nowTime);
		return "/fundTarget/fundTargetForm";
	}
	
	
	//??? ????????????
	@PostMapping("/form")
	private String create(@Valid TargetForm targetForm,BindingResult bindingResult,
			@RequestParam("categorie")Integer cid, @RequestParam(value="imgPath", defaultValue = "x")String imgPath
			,@RequestParam(value="file", defaultValue = "x")MultipartFile files, Model model,Principal principal) throws IllegalStateException, IOException {
		
		String startTime = targetForm.getStartDate();
		Categorie categorie = categorieService.findById(cid);
		List<Categorie> cList = categorieService.findAll();		
		
		if(imgPath.equals("x") && files.isEmpty()) {
			bindingResult.reject("noImgError", "???????????? ????????? ?????????");
		}
		
		if(bindingResult.hasErrors()) {
			model.addAttribute("cList",cList);
			return "/fundTarget/fundTargetForm";
		}
		
		Optional<FundUser> user = fundUserService.findByuserName(principal.getName());
	
	
		if(!imgPath.equals("x") && files.isEmpty()) {
			fundTargetService.createimg(
					targetForm.getSubject(), 
					targetForm.getContent(), 
					targetForm.getAertiest(),
					targetForm.getPlace(),
					targetForm.getRuntime(),
					targetForm.getFundDurationE(),
					startTime,
					targetForm.getMinFund(),
					targetForm.getFundAmount(),
					categorie,
					imgPath,
					user.get()
					);
		}else if(!files.isEmpty()){
			
			String savePath = fileService.saveFile(files);
			
			fundTargetService.createfile(
					targetForm.getSubject(), 
					targetForm.getContent(), 
					targetForm.getAertiest(),
					targetForm.getPlace(),
					targetForm.getRuntime(),
					targetForm.getFundDurationE(),
					startTime,
					targetForm.getMinFund(),
					targetForm.getFundAmount(),
					categorie,
					savePath,
					user.get()
					);
		}
		
		
		return "redirect:/";
	}
	
	//??? ?????? ??????
	@RequestMapping("")
	public String showList(Model model,@RequestParam(value = "page", defaultValue="0") int page,
			@RequestParam(value = "cate", defaultValue="0")Integer cateId) {
		

		//?????? ???????????? ??????
		if(cateId == 0) {
			Page<FundBoardTarget> targetList = fundTargetService.findAll(page);
			List<Categorie> cList = categorieService.findAll();	
			
			model.addAttribute("page",page);
			model.addAttribute("cate",cateId);
			model.addAttribute("cList", cList);
			model.addAttribute("targetList", targetList);
			return "fundTarget/fundTargetList";
		//?????? ???????????? ??????
		}else {
			Categorie categorie = categorieService.findById(cateId);
			Page<FundBoardTarget> targetList = fundTargetService.findByCategorie(categorie, page);
			List<Categorie> cList = categorieService.findAll();	
			
			model.addAttribute("page",page);
			model.addAttribute("cate",cateId);
			model.addAttribute("cList", cList);
			model.addAttribute("targetList", targetList);
			return "fundTarget/fundTargetList";
		}

	}

	//????????? ?????????
	@RequestMapping("/detail/{id}")
	public String showDetail(Model model, @PathVariable("id")Integer id,Integer alertId, Principal principal) {
		FundBoardTarget fundBoardTarget = fundTargetService.findById(id);
		List<Answer> aList = answerService.findByFundBoardTarget(fundBoardTarget);
		List<FundTargetList> ftList = fundTargetListService.findByFundBoardTarget(fundBoardTarget);
		
		//??????
		FundBoardTarget nick = fundTargetService.findById(id);
		List<Sale> sale = saleRepository.findByFundBoardTarget(nick.getSubject());
		for(int i=0; i<sale.size(); i++){
			sale.get(i).getPayCode();
			model.addAttribute("payCode",sale.get(i).getPayCode());
		}

		
		//????????????
		if(alertId != null) {
			alertService.deleteAlert(alertId);
		}
		
		//?????? ?????? ??????
		boolean result = false;
		if(principal != null) {
			for(FundTargetList e : ftList) {
				String username = e.getFundUser().getUsername();
				String loginName = principal.getName();
				if(username.equals(loginName)) {
					result = true;
				}
			}
		}
		
		
		model.addAttribute("result", result);
		model.addAttribute("aList", aList);
		model.addAttribute("fundBoardTarget", fundBoardTarget);
		return "/fundTarget/fundTargetDetail";
	}
	
	//????????? ?????????
	@GetMapping("/img/{id}")
	@ResponseBody
	public Resource showImg(@PathVariable("id")Integer id) throws IOException {
		FundBoardTarget target = fundTargetService.findById(id);
		String imgPath = target.getFilePath();
		
		return new UrlResource("file:" + imgPath);
	}
	
	
	
	//???????????? ??????
	@RequestMapping("/delete/{id}")
	public String deleteTarget(@PathVariable("id")Integer id) throws Exception {

		//??????
		FundBoardTarget nick = fundTargetService.findById(id);
		List<Sale> sale = saleRepository.findByFundBoardTarget(nick.getSubject());
		for(int i=0; i<sale.size(); i++){
			if(sale.get(i).getCheckin().equals("????????????")) {
				cancelsController.totalCancel(sale.get(i).getPayCode(),"????????? ??????");
				cancelsService.totalCancelInfo(sale.get(i).getOrederId(), Integer.valueOf(sale.get(i).getPayMoney()).intValue(), sale.get(i).getOrderName(), 
						"????????? ??????",sale.get(i).getFundUser(),sale.get(i).getUsername());
			}
		}
		
		log.info("?????????????????? ?????????");
		//??????????????? ??????
		List<FundTargetList> fList = fundTargetListService.findByFundBoardTarget(nick);
		for(int i=0;i>fList.size();i++) {
			fundTargetListService.delete(fList.get(i).getFundUser(), nick);
		}
		//?????? ?????? ??????
		alertService.deleteTargetThenAlert(fList);
		
		fundTargetService.delete(id);
		return "redirect:/";
	}
	
	
}
